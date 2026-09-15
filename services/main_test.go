package main

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"worker-go/pb"
)

var tcgDexBaseURL string

func TestGetPokemonCard_Success(t *testing.T) {
	mockServer := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(TcgDexResponse{
			ID:    "base1-4",
			Name:  "Charizard",
			Image: "https://assets.tcgdex.net/en/base/base1/4",
		})
	}))
	defer mockServer.Close()

	tcgDexBaseURL = mockServer.URL + "/"

	s := &server{}
	resp, err := s.GetPokemonCard(context.Background(), &pb.CardRequest{CardId: "base1-4"})

	if err != nil {
		t.Fatalf("erro inesperado: %v", err)
	}
	if !resp.Success {
		t.Fatal("esperava Success = true")
	}
	if resp.Name != "Charizard" {
		t.Errorf("esperava nome Charizard, veio %s", resp.Name)
	}
}
