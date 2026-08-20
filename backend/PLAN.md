# Plano de Implementação: LookBuy Backend

---

## Fase 1: Setup do Ambiente e Schemas do Protocolo WS
- [ ] Inicializar estrutura de diretórios (`app/api`, `app/core`, `app/services`, `app/schemas`, `tests`).
- [ ] Configurar gerenciamento de ambiente com `pyproject.toml` e `uv`.
- [ ] Implementar schemas Pydantic discriminados para as mensagens WebSocket (`INITIAL_SCAN`, `CLARIFICATION_REQUIRED`, `CLARIFICATION_ANSWER`, `LOOKUP_COMPLETE`, `ERROR`).
- [ ] **Critério de Validação:** Testes unitários validando serialização e parsing de todos os payloads JSON em `tests/test_schemas.py`.

---

## Fase 2: Ciclo de Vida da Conexão WebSocket
- [ ] Criar endpoint `/ws/lookup` no FastAPI usando `APIRouter`.
- [ ] Implementar gerenciador de sessão para manter o estado da requisição (esperando imagem, aguardando clarificação, finalizado).
- [ ] Configurar tratamento de encerramento de conexão gracioso (código de fechamento 1000).
- [ ] **Critério de Validação:** Teste de integração usando `pytest` e `httpx.AsyncClient` / `TestClient` simulando o handshake e troca de mensagens mockadas.

---

## Fase 3: Módulo de Visão & OCR (VLM)
- [ ] Criar serviço abstrato `VisionService` em `app/services/vision.py`.
- [ ] Implementar decodificação do buffer de imagem recebido do payload `INITIAL_SCAN`.
- [ ] Adicionar lógica de classificação de confiança (Cenários A, B e C).
- [ ] Implementar mock/fallback para ambiente de testes sem GPU.
- [ ] **Critério de Validação:** Testes cobrindo os 3 branches de decisão (Alta Confiança, Ambiguidade, Falha).

---

## Fase 4: Módulo de Scraping & Busca de Preços
- [ ] Implementar serviço assíncrono de busca de preços em `app/services/scraper.py`.
- [ ] Configurar parser de normalização de preços e moedas (BRL).
- [ ] Integrar fallback para casos em que o scraper não encontrar preços ou houver timeout de rede.
- [ ] **Critério de Validação:** Testes de scraping com fixtures HTML mockadas garantindo extração consistente.

---

## Fase 5: Integração de Ponta a Ponta e Loop de Clarificação
- [ ] Conectar `VisionService` e `ScraperService` dentro do handler do WebSocket.
- [ ] Implementar o fluxo de pausa para pergunta de clarificação e retomada após resposta do cliente.
- [ ] Adicionar formatação de texto final otimizada para síntese por TTS.
- [ ] **Critério de Validação:** Teste end-to-end simulando o Cenário B completo (Upload $\rightarrow$ Pergunta $\rightarrow$ Resposta $\rightarrow$ Preço Final).