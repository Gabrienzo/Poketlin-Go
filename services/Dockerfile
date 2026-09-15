FROM golang:1.23 AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o worker .

FROM gcr.io/distroless/static-debian12
COPY --from=build /app/worker /worker
EXPOSE 50051
ENTRYPOINT ["/worker"]