package main

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"worker-go/pb"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

const (
	tcgDexBase       = "https://api.tcgdex.net/v2/en"
	defaultPageSize  = 20
	maxPageSize      = 100
	maxSetsPerSearch = 5 // limita quantas edições uma busca por set consulta
)

var httpClient = &http.Client{Timeout: 10 * time.Second}

type cardBrief struct {
	ID      string `json:"id"`
	LocalID string `json:"localId"`
	Name    string `json:"name"`
	Image   string `json:"image"`
}

type setBrief struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type setDetail struct {
	ID    string      `json:"id"`
	Name  string      `json:"name"`
	Cards []cardBrief `json:"cards"`
}

// Cache simples com validade, para o scroll infinito não rebater na TCGDex a cada página.
type ttlItem[V any] struct {
	value V
	at    time.Time
}

type ttlCache[V any] struct {
	mu    sync.Mutex
	items map[string]ttlItem[V]
	ttl   time.Duration
}

func newTTLCache[V any](ttl time.Duration) *ttlCache[V] {
	return &ttlCache[V]{items: map[string]ttlItem[V]{}, ttl: ttl}
}

func (c *ttlCache[V]) get(key string, load func() (V, error)) (V, error) {
	c.mu.Lock()
	defer c.mu.Unlock()
	if it, ok := c.items[key]; ok && time.Since(it.at) < c.ttl {
		return it.value, nil
	}
	v, err := load()
	if err != nil {
		var zero V
		return zero, err
	}
	c.items[key] = ttlItem[V]{value: v, at: time.Now()}
	return v, nil
}

var (
	setsList   = newTTLCache[[]setBrief](time.Hour)
	setsDetail = newTTLCache[setDetail](time.Hour)
)

func getJSON(ctx context.Context, rawURL string, out any) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, rawURL, nil)
	if err != nil {
		return err
	}
	resp, err := httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("TCGDex respondeu %d para %s", resp.StatusCode, rawURL)
	}
	return json.NewDecoder(resp.Body).Decode(out)
}

func (s *server) SearchCards(ctx context.Context, req *pb.SearchCardsRequest) (*pb.SearchCardsResponse, error) {
	name := strings.TrimSpace(req.GetName())
	setQuery := strings.TrimSpace(req.GetSet())
	if name == "" && setQuery == "" {
		return nil, status.Error(codes.InvalidArgument, "informe name e/ou set")
	}

	page := int(req.GetPage())
	if page < 1 {
		page = 1
	}
	size := int(req.GetPageSize())
	if size < 1 {
		size = defaultPageSize
	}
	if size > maxPageSize {
		size = maxPageSize
	}

	sets, err := setsList.get("all", func() ([]setBrief, error) {
		var lista []setBrief
		err := getJSON(ctx, tcgDexBase+"/sets", &lista)
		return lista, err
	})
	if err != nil {
		return nil, status.Errorf(codes.Unavailable, "falha ao consultar a TCGDex: %v", err)
	}

	var briefs []cardBrief
	var hasMore bool
	if setQuery != "" {
		briefs, hasMore, err = searchBySet(ctx, sets, setQuery, name, page, size)
	} else {
		briefs, hasMore, err = searchByName(ctx, name, page, size)
	}
	if err != nil {
		return nil, status.Errorf(codes.Unavailable, "falha ao consultar a TCGDex: %v", err)
	}

	cards := make([]*pb.CardResponse, 0, len(briefs))
	for _, b := range briefs {
		cards = append(cards, &pb.CardResponse{
			Id:       b.ID,
			Name:     b.Name,
			ImageUrl: thumbnailURL(b.Image),
			SetName:  setNameOf(sets, b),
			Success:  true,
		})
	}
	return &pb.SearchCardsResponse{Cards: cards, HasMore: hasMore}, nil
}

func searchByName(ctx context.Context, name string, page, size int) ([]cardBrief, bool, error) {
	endpoint := fmt.Sprintf("%s/cards?name=%s&pagination:page=%d&pagination:itemsPerPage=%d",
		tcgDexBase, url.QueryEscape(name), page, size)
	var briefs []cardBrief
	if err := getJSON(ctx, endpoint, &briefs); err != nil {
		return nil, false, err
	}
	// A API não informa o total: página cheia significa que talvez haja mais.
	return briefs, len(briefs) == size, nil
}

func searchBySet(ctx context.Context, sets []setBrief, setQuery, name string, page, size int) ([]cardBrief, bool, error) {
	edicao := strings.ToLower(setQuery)
	var all []cardBrief
	consultados := 0

	for _, st := range sets {
		if !strings.Contains(strings.ToLower(st.Name), edicao) && !strings.EqualFold(st.ID, setQuery) {
			continue
		}
		if consultados == maxSetsPerSearch {
			break
		}
		consultados++

		detail, err := setsDetail.get(st.ID, func() (setDetail, error) {
			var d setDetail
			err := getJSON(ctx, tcgDexBase+"/sets/"+url.PathEscape(st.ID), &d)
			return d, err
		})
		if err != nil {
			return nil, false, err
		}
		all = append(all, detail.Cards...)
	}

	if name != "" {
		trecho := strings.ToLower(name)
		filtradas := make([]cardBrief, 0, len(all))
		for _, c := range all {
			if strings.Contains(strings.ToLower(c.Name), trecho) {
				filtradas = append(filtradas, c)
			}
		}
		all = filtradas
	}

	inicio := (page - 1) * size
	if inicio >= len(all) {
		return nil, false, nil
	}
	fim := inicio + size
	hasMore := fim < len(all)
	if fim > len(all) {
		fim = len(all)
	}
	return all[inicio:fim], hasMore, nil
}

// O id da carta é "<id do set>-<localId>", então dá para descobrir o set pelo id.
func setNameOf(sets []setBrief, b cardBrief) string {
	setID := strings.TrimSuffix(b.ID, "-"+b.LocalID)
	for _, s := range sets {
		if s.ID == setID {
			return s.Name
		}
	}
	return setID // sem nome conhecido, usa o id do set (a edição nunca fica vazia)
}

func thumbnailURL(image string) string {
	if image == "" {
		return ""
	}
	return image + "/low.png"
}
