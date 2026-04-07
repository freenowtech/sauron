package main

import (
    "fmt"
    "runtime"
    "gopkg.in/yaml.v2"
)

func main() {
    fmt.Println("Hello SBOM")
    fmt.Printf("OS: %s\n", runtime.GOOS)
    fmt.Printf("Arch: %s\n", runtime.GOARCH)
    fmt.Printf("CPUs: %d\n", runtime.NumCPU())
    fmt.Printf("Go Version: : %s\n", runtime.Version())
}