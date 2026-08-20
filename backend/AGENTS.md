## 1. Visão Geral do Projeto
Breve resumo do que o projeto faz e stack principal (ex: Python/FastAPI, TypeScript/Next.js, etc.).

## 2. Comandos Principais (`uv`)

> **Importante:** Sempre execute comandos de ambiente usando `uv run`. Nunca use `pip` ou `python` soltos.

- **Instalar/Sincronizar Dependências:**
  ```bash
  uv sync
  ```
- **Executar Servidor de Desenvolvimento:**
  ```bash
  uv run uvicorn app.main:app --reload --port 8000
  # ou caso use o CLI do FastAPI:
  # uv run fastapi dev app/main.py
  ```
- **Adicionar Nova Dependência:**
  ```bash
  uv add <nome-do-pacote>
  uv add --dev <nome-do-pacote>  # Dependências de dev
  ```
---

## 3. Arquitetura e Fluxo (Mermaid)

### Diagrama de fluxo
```mermaid
flowchart TD
    %% SUBGRAPH 1: SMART GLASSES
    subgraph Glasses[" Smart Glasses (Hardware)"]
        Start([Standby]) --> VoiceTrigger[Gatilho de Voz:\n'LookBuy, quanto custa isto?']
        VoiceTrigger --> CaptureFrame[Captura Frame Pontual FPV]
        PlayAudioOut[Reprodução de Áudio nos Alto-falantes] --> DiscardLocalMem[Descarte de Buffer Local]
        DiscardLocalMem --> EndState([Standby])
    end

    %% SUBGRAPH 2: COMPANION APP (LIGHTWEIGHT / ON-DEVICE)
    subgraph CompanionApp[" Companion App (Mobile / On-Device)"]
        CaptureFrame -->|BLE / Wi-Fi Direct| OffloadToApp[Recebe Frame Comprimido]
        
        OffloadToApp --> PrivacyFilter["Filtro de Privacidade Local (NPU/GPU)\n(Detecção e Blur de Rostos de Terceiros)"]
        
        PrivacyFilter -->|Payload Seguro: Imagem + Meta| SendToBackend[Dispara Requisição HTTP/WS]
        
        %% Interações de Voz On-Device
        LocalTTS_Fail["TTS Nativo Local\n('Não consegui identificar...')"] --> PlayAudioOut
        LocalTTS_Ask["TTS Nativo Local\n('Pergunta de Clarificação')"] --> PlayAudioOut
        
        MicCapture[Microfone captura resposta] --> OnDeviceSTT["STT Local (Nativo / Whisper-Tiny)"]
        OnDeviceSTT -->|Texto Transcrito| SendVoiceClarification[Envia Resposta ao Backend]
        
        LocalTTS_Success["TTS Nativo Local\n(Sintetiza Preço/Detalhes)"] --> PlayAudioOut
    end

    %% SUBGRAPH 3: BACKEND (FASTAPI / HEAVY COMPUTE)
    subgraph Backend[" Backend (FastAPI / GPUs & Workers)"]
        SendToBackend --> VLMInference["Pipeline de Visão & OCR\n(VLM / OCR de Rótulo e Embalagem)"]
        
        VLMInference --> CheckConfidence{Nível de Certeza?}
        
        %% Cenário C: Falha
        CheckConfidence -- "Baixa / Ruído" --> RespFail[Retorna Payload de Erro]
        RespFail --> LocalTTS_Fail

        %% Cenário B: Ambiguidade
        CheckConfidence -- "Ambiguidade (Variação)" --> LLMClarification["LLM: Gera Pergunta de Clarificação"]
        LLMClarification --> LocalTTS_Ask
        LocalTTS_Ask -.-> MicCapture
        SendVoiceClarification --> LLMResolve["LLM: Resolve Entidade Final"]
        LLMResolve --> PriceScraper

        %% Cenário A: Alta Confiança
        CheckConfidence -- "Alta Confiança" --> PriceScraper["Scraper / Worker de Preços\n(Busca Web / Crawl4AI / APIs Regionais)"]

        PriceScraper --> CheckAPI{Preço Encontrado?}
        
        CheckAPI -- "Sim" --> FormatSuccess["LLM / Service:\nFormata Preço e Promoção"]
        CheckAPI -- "Não" --> FormatFallback["LLM / Service:\nFormata Item sem Preço"]
        
        FormatSuccess --> SendSuccessPayload[Retorna Texto Formatado]
        FormatFallback --> SendSuccessPayload
        SendSuccessPayload --> LocalTTS_Success
    end
```

### Diagrama de sequencia
```mermaid
sequenceDiagram
    autonumber
    actor User as Usuário / Óculos
    participant App as Companion App
    participant WS as FastAPI WebSocket (/ws/lookup)
    participant VLM as Pipeline VLM & OCR
    participant Scraper as Worker de Preços

    %% 1. Início de Sessão e Conexão
    User->>App: Gatilho de Voz + Frame FPV
    Note over App: Aplica Filtro de Privacidade Local (Blur em rostos)
    App->>WS: 1. Abre Conexão WebSocket (Session Handshake)
    WS-->>App: Conexão Estabelecida (ACK)
    
    %% 2. Envio do Frame Processado
    App->>WS: Envia Payload Inicial (JSON + Imagem Base64 / Binário)
    
    %% 3. Processamento no Backend
    WS->>VLM: Inferência Multimodal (Identificação + OCR)
    
    alt Cenário C: Confiança Insuficiente / Ruído
        VLM-->>WS: Falha de Identificação
        WS-->>App: WS Message: { status: "error", message: "Não consegui identificar..." }
        App->>User: TTS Nativo Local (Erro)
        App->>WS: Fecha Conexão (Close Code 1000)

    else Cenário B: Ambiguidade (Ex: 250g vs 500g)
        VLM-->>WS: Ambiguidade detectada
        WS-->>App: WS Message: { status: "clarification_needed", question: "É o pacote de 250g ou 500g?" }
        App->>User: TTS Nativo Local (Pergunta)
        User->>App: Resposta por Voz
        Note over App: STT Local (Transcreve áudio -> texto)
        App->>WS: WS Message: { type: "clarification_response", answer: "250g" }
        WS->>Scraper: Busca Preço para entidade resolvida
        Scraper-->>WS: Preço encontrado
        WS-->>App: WS Message: { status: "success", product: "Café 250g", price: "R$ 14,90" }
        App->>User: TTS Nativo Local (Sucesso)
        App->>WS: Fecha Conexão (Close Code 1000)

    else Cenário A: Alta Confiança
        VLM-->>WS: Produto identificado com precisão
        WS-->>App: WS Message: { status: "processing", message: "Buscando melhores preços..." }
        WS->>Scraper: Busca de Preço / Scraping
        Scraper-->>WS: Preços extraídos
        WS-->>App: WS Message: { status: "success", product: "...", price: "..." }
        App->>User: TTS Nativo Local (Sucesso)
        App->>WS: Fecha Conexão (Close Code 1000)
    end
```
---
## 4. Padrões de Código e FastAPI

- **Tipagem e Schemas:** Use `pydantic.BaseModel` e type hints modernos (`list[str]`, `str | None` em vez de `typing.Optional`/`typing.List`).
- **Injeção de Dependências:** Sempre use `Annotated[..., Depends(...)]` para dependências de rotas.
- **Async/Await:** Mantenha handlers de rota assíncronos (`async def`). Se chamar I/O bloqueante, delegue para threads ou use bibliotecas nativamente assíncronas.
- **Tratamento de Erros:** Lance `HTTPException` com códigos de status semânticos ou use handlers de exceção personalizados.
- **Variáveis de Ambiente:** Use `pydantic-settings` para gerenciar configurações.

---

## 5. Regras para o Agente (Do's & Don'ts)

- **FAÇA:**
  - Sempre verifique se a acao atual esta presente no RoadMap
  - Modo de Execução e Roadmap:
    - Consulte sempre o arquivo `PLAN.md` antes de iniciar uma nova tarefa.
    - Siga as fases estritamente em ordem sequencial.
    - Nunca avance para o próximo passo sem rodar a suíte de testes correspondente com `uv run pytest`.
    - Ao concluir uma sub-tarefa, marque a caixa no `PLAN.md` com `[x]`.
  - Mantenha a separação de responsabilidades (Rotas $\rightarrow$ Services $\rightarrow$ Repositories).

- **NÃO FAÇA:**
  - Não use `pip install` ou manipule `requirements.txt` manualmente (use sempre `uv add`).
  - Não coloque regras de negócio complexas ou consultas diretas ao banco dentro das funções dos endpoints (`routers`).
  - Não desative verificações de tipo com `# type: ignore` sem justificativa clara.