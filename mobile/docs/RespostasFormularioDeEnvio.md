# Respostas para o Formulário de Envio — LookBuy

Este documento reúne respostas prontas com base na proposta, arquitetura, diagramas e código do repositório. Campos pessoais, uploads e vídeo que não estão no projeto foram marcados como **[PREENCHER]**.

> **Nota de transparência:** o protótipo Android já integra Meta DAT/MDK, câmera, STT, TTS e o fluxo de captura. O `VisionClient` e os repositórios de preços retornam dados mockados; VLM/OCR, consulta real de preços e blur de rostos constam como a próxima integração da arquitetura. Não apresente essas três partes como implementadas se a avaliação exigir comprovação em execução.

## Página 1 — Identificação

### E-mail *

**[PREENCHER: e-mail de contato da equipe]**

### Nome da equipe *

**[PREENCHER: nome da equipe na inscrição]**

### Trilha temática *

**Produtividade**

### Título da solução *

**LookBuy**

### Resumo em uma frase *

Assistente mãos-livres que identifica produtos pela câmera e informa por voz seu preço estimado para consumidores em lojas físicas.

## Página 2 — Seção A: Documento estruturado

### A1 — O problema *

Em compras presenciais, comparar preços ou descobrir o valor de produtos sem etiqueta exige tirar o celular do bolso, abrir um app e escanear ou digitar. Isso interrompe a compra e exclui pessoas com baixa visão ou mobilidade reduzida que precisam de uma alternativa simples e acessível.

### A2 — Usuário-alvo *

Consumidores que fazem compras presenciais, especialmente pessoas com baixa visão, idosos ou quem está com as mãos ocupadas com carrinho e produtos. Usam a solução pontualmente, várias vezes em uma ida ao supermercado, farmácia ou loja, para decidir se vale comprar um item.

### A3 — Walkthrough de interação (caminho principal) *

1. No supermercado, a pessoa olha para um produto e segura o botão de microfone no companion app (ou usa o gatilho de voz dos óculos).
2. O `SpeechRecognizer` do Android transcreve “LookBuy, quanto custa isto?” e o app aciona a câmera DAT; em debug, o MDK usa a câmera traseira do celular como visão dos óculos.
3. O `DatCameraClient` captura uma foto; se `capturePhoto()` não estiver disponível no MDK, usa o último frame YUV válido da prévia como fallback.
4. Antes do envio planejado ao backend, a camada `PrivacyFilter` normaliza a rotação do frame e é o ponto de aplicação do blur local de rostos; o payload seguro segue para VLM/OCR e consulta de preços.
5. Com identificação de alta confiança, o backend associa marca, produto e embalagem à busca de preço regional. No protótipo, o `VisionClient` simula esse retorno para validar a jornada.
6. O `ProcessProductLookUseCase` atualiza a interface com nome e preço estimado e o `TextToSpeech` responde em PT-BR: “Encontrei… por aproximadamente…”.
7. O áudio é roteado por HFP/SCO para fones ou óculos Bluetooth; sem acessório, o Android usa microfone e alto-falante do celular.

### A4 — Walkthrough de exceção (quando dá errado) *

Se o rótulo estiver desfocado, encoberto ou a identificação tiver baixa confiança, o VLM não retorna preço. O sistema reconhece a incerteza pelo limiar de confiança e responde por TTS: “Não consegui identificar com segurança. Aproxime-se do rótulo ou tente outro ângulo.” Assim, não inventa uma resposta. Se houver dúvida entre variantes, pergunta por voz a embalagem ou peso antes da consulta.

### A5 — Decisões técnicas e trade-offs

#### A5[1].a — A decisão *

Capturamos foto sob demanda em vez de vídeo contínuo.

#### A5[1].b — Por que esse lado *

Uma imagem por consulta basta para OCR e identificação; reduz câmera, rádio e bateria dos óculos durante uma compra.

#### A5[1].c — O que isso custou *

Podemos perder um frame desfocado. Mitigamos com nova captura por voz e instrução para aproximar ou reposicionar o olhar.

#### A5[2].a — A decisão *

Usamos VLM/OCR no backend em vez de modelo pesado nos óculos.

#### A5[2].b — Por que esse lado *

Preserva a bateria e o hardware limitado dos óculos, permitindo OCR e reconhecimento de embalagem mais precisos.

#### A5[2].c — O que isso custou *

Há dependência de rede e latência. Sem conexão, informamos a indisponibilidade e não afirmamos um preço não verificado.

#### A5[3].a — A decisão *

Roteamos áudio com HFP do Android em vez de depender do DAT.

#### A5[3].b — Por que esse lado *

O DAT não gerencia áudio; HFP permite STT/TTS em fones e nos óculos, com fallback no próprio celular.

#### A5[3].c — O que isso custou *

Exige acessório Bluetooth compatível para a experiência nos óculos. Sem ele, usamos microfone e alto-falante do celular.

#### A5[4].a — A decisão (opcional)

Usamos o último frame da prévia quando `capturePhoto()` falha.

#### A5[4].b — Por que esse lado (opcional)

O MDK separa stream e captura; o fallback mantém a demonstração funcional com a câmera traseira do celular.

#### A5[4].c — O que isso custou (opcional)

O frame pode estar menos nítido que uma foto dedicada. Exibimos a prévia e permitimos repetir a consulta.

#### A5[5].a — A decisão (opcional)

Usamos STT/TTS nativos em vez de modelos de voz embarcados.

#### A5[5].b — Por que esse lado (opcional)

Reduz o tempo de implementação e valida rapidamente a interação de voz em PT-BR no protótipo Android.

#### A5[5].c — O que isso custou (opcional)

O STT atual pode depender do serviço do dispositivo ou rede. Vosk ou Whisper-Tiny são alternativas offline futuras.

## Página 7 — Âncora de originalidade

### A6[1].a — Concorrente 1: nome *

Meta AI nos Ray-Ban Meta

### A6[1].b — Concorrente 1: diferencial *

O LookBuy especializa a visão em produto, variante e preço regional, usando pergunta de confirmação para não informar valor de item errado.

### A6[2].a — Concorrente 2: nome *

Google Lens

### A6[2].b — Concorrente 2: diferencial *

O LookBuy foi desenhado para consulta por voz, primeira pessoa e resposta falada, sem exigir manipulação da tela durante a compra.

## Página 8 — Mapeamento dos 5 checkpoints obrigatórios

### A7.1 — Uso de IA *

O fluxo tem cliente de visão e prevê VLM/OCR para identificar produto, marca e embalagem, além de LLM para clarificação. Hoje o `VisionClient` é mockado.

### A7.2 — Câmera ou microfone (canal de entrada) *

O microfone recebe a pergunta e a câmera DAT captura o item sob demanda. Em debug, o MDK usa a câmera traseira do celular como visão dos óculos.

### A7.3 — Output por áudio *

`TextToSpeech` nativo em PT-BR fala processamento, erro e resultado. O `DatAudioClient` prioriza HFP/SCO e usa o áudio do celular como fallback.

### A7.4 — Privacidade e dados *

A arquitetura prevê frame temporário, descarte após inferência e blur local de rostos antes do envio. O hook existe; a detecção/blur real ainda será integrada.

### A7.5 — Eficiência de bateria *

Usamos captura pontual, stream MEDIUM a 15 fps e processamento pesado fora dos óculos. A prévia converte no máximo dois frames por segundo.

## Página 9 — Seção B: Diagrama de arquitetura

### B1 — Imagem do diagrama *

**[PREENCHER: exportar o diagrama Mermaid em PNG, JPG, SVG ou PDF e fazer upload.]**

Fonte recomendada: o fluxo de arquitetura em [LookBuy_Arquitetura.md](LookBuy_Arquitetura.md) ou em [FluxoDeSequencia](FluxoDeSequencia).

### B2 — Código Mermaid *

**[PREENCHER: exportar o código Mermaid em TXT, DOCX ou PDF e fazer upload.]**

O código-fonte está em [LookBuy_Arquitetura.md](LookBuy_Arquitetura.md), [FluxoDeSequencia](FluxoDeSequencia) e [DiagramaDeSequencia](DiagramaDeSequencia). Ele contém os nós de IA/VLM, câmera, áudio, privacidade e eficiência de bateria.

## Página 10 — Seção C: Vídeo-pitch

### C1 — Vídeo-pitch elevator *

**[PREENCHER: URL do vídeo não listado no YouTube, Drive ou Vimeo.]**

### C2 — Duração do vídeo *

**[PREENCHER após a gravação: selecionar “Entre 2 e 3 min”.]**

## Página 11 — Avaliação do Ideathon

### De 0 a 10, o quanto o Ideathon de hoje foi útil para a equipe? *

**[PREENCHER: avaliação pessoal da equipe.]**

### O que foi mais útil?

**[PREENCHER: resposta pessoal da equipe.]**

### O que melhorar?

**[PREENCHER: resposta pessoal da equipe.]**

## Página 12 — Confirmações finais

### Coerência entre artefatos *

- [x] Confirmamos que o documento, o diagrama e o vídeo descrevem a mesma solução de forma coerente.

### Autoria e uso de IA *

- [x] Confirmamos que as decisões técnicas e o conteúdo são da equipe. Ferramentas de IA podem ter apoiado a redação, mas o conteúdo reflete nosso trabalho.

### Escopo mantido *

- [x] Confirmamos que esta entrega mantém o escopo do projeto/ideia submetido na inscrição.
- [ ] Mudamos o escopo do projeto/ideia submetido.

## Observações de submissão

- Gere os uploads da seção B a partir dos diagramas Mermaid existentes.
- Grave e hospede o vídeo antes de preencher a seção C.
- Complete apenas as três respostas da avaliação do Ideathon com a experiência real da equipe.
- Caso a banca exija implementação efetiva de todos os checkpoints, concluam a integração de VLM/OCR, preço remoto e blur de rostos antes da apresentação; esses componentes ainda são mocks ou pontos de extensão no código atual.
