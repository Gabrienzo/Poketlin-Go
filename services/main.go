package main

import (
	"context"
	"encoding/json"
	"log"
	"net"
	"net/http"
	"time"

	"worker-go/pb"

	"google.golang.org/grpc"
)

type TcgDexResponse struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	Image string `json:"image"`
	Set   struct {
		Name string `json:"name"`
	} `json:"set"`
}

type server struct {
	pb.UnimplementedTcgDataServiceServer
}

func (s *server) GetPokemonCard(ctx context.Context, req *pb.CardRequest) (*pb.CardResponse, error) {
	url := "https://api.tcgdex.net/v2/en/cards/" + req.GetCardId()

	client := http.Client{Timeout: 10 * time.Second}
	resp, err := client.Get(url)
	if err != nil || resp.StatusCode != 200 {
		return &pb.CardResponse{Success: false}, nil
	}
	defer resp.Body.Close()

	var tcgData TcgDexResponse
	if err := json.NewDecoder(resp.Body).Decode(&tcgData); err != nil {
		return &pb.CardResponse{Success: false}, nil
	}

	return &pb.CardResponse{
		Id:       tcgData.ID,
		Name:     tcgData.Name,
		ImageUrl: tcgData.Image + "/high.png",
		SetName:  tcgData.Set.Name,
		Success:  true,
	}, nil
}

func main() {
	lis, err := net.Listen("tcp", ":50051")
	if err != nil {
		log.Fatalf("Falha ao escutar a porta: %v", err)
	}

	s := grpc.NewServer()
	pb.RegisterTcgDataServiceServer(s, &server{})

	if err := s.Serve(lis); err != nil {
		log.Fatalf("Falha ao iniciar o servidor: %v", err)
	}
}
