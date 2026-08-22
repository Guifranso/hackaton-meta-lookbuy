# Respostas para o Formulário de Envio — LookBuy

> **Nota de transparência:** o protótipo Android já integra Meta DAT/MDK, câmera, STT, TTS e o fluxo de captura. O `VisionClient` e os repositórios de preços retornam dados mockados; VLM/OCR, consulta real de preços e blur de rostos constam como a próxima integração da arquitetura. Não apresente essas três partes como implementadas se a avaliação exigir comprovação em execução.

**Produtividade**

### Título da solução *

**LookBuy**

### Resumo em uma frase *

Assistente de compras por voz que identifica produtos pela câmera e informa por voz seu preço estimado do produto observado.

## Página 2 — Seção A: Documento estruturado

### A1 — O problema *

Ao se deparar com um produto, o consumidor frequentemente decide sem contexto suficiente para avaliar se vale a pena: preço médio, variações entre embalagens e alternativas similares. Buscar essas informações no celular interrompe o momento da decisão e torna comparações rápidas menos práticas.

### A2 — Usuário-alvo *

Pessoas que querem avaliar melhor um produto antes de decidir comprá-lo, principalmente em situações presenciais como supermercados, farmácias e lojas. Usam o LookBuy pontualmente, quando desejam comparar preço médio, embalagem, variante e alternativas similares sem interromper o momento da decisão.

### A3 — Walkthrough de interação (caminho principal) *

1. A pessoa olha para um produto e segura o botão de microfone no companion app (ou usa o gatilho de voz dos óculos).
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

Usamos todo processamento complexo concentrado ao backend.

#### A5[2].b — Por que esse lado *

A bateria limitada dos óculos e a capacidade limitada de processamento disponível no contexto do app companion

#### A5[2].c — O que isso custou *

Há dependência de rede e latência. Sem conexão, informamos a indisponibilidade e não afirmamos um preço não verificado.

#### A5[3].a — A decisão *

Utilizar avaliação multi modal para identificação dos objetos de interesse

#### A5[3].b — Por que esse lado *

Para evitar o uso de modelos VLM muito pesados, e gastos de computação muito elevados com re processamentos.

#### A5[3].c — O que isso custou *

Maior complexidade ao orquestrar as entradas e saídas em paralelo.

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

O STT atual pode depender do serviço do dispositivo ou rede. Whisper-Tiny e uma alternativa para maior internacionalidade e processamento de STT offline dentro do App companion.

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

Ao utilizar os oculos apenas para capturas pontuais e feedbacks de audio, conseguimos concentrar o uso de bateria principalmente ao app companion, deixando a vida util diaria da bateria intocada
## Página 9 — Seção B: Diagrama de arquitetura

### B1 — Imagem do diagrama *

**[PREENCHER: exportar o diagrama Mermaid em PNG, JPG, SVG ou PDF e fazer upload.]**

Fonte recomendada: o fluxo de arquitetura em [LookBuy_Arquitetura.md](LookBuy_Arquitetura.md) ou em [FluxoDeSequencia](FluxoDeSequencia).

### B2 — Código Mermaid *

**[PREENCHER: exportar o código Mermaid em TXT, DOCX ou PDF e fazer upload.]**

O código-fonte está em [LookBuy_Arquitetura.md](LookBuy_Arquitetura.md), [FluxoDeSequencia](FluxoDeSequencia) e [DiagramaDeSequencia](DiagramaDeSequencia). Ele contém os nós de IA/VLM, câmera, áudio, privacidade e eficiência de bateria.

## Página 11 — Avaliação do Ideathon

### De 0 a 10, o quanto o Ideathon de hoje foi útil para a equipe? *

6

### O que foi mais útil?

conseguimos tirar bastante dúvidas sobre o hackaton e obter uma visão geral do que deveriamos entregar, além de obtermos uma visão geral das equipes que estamos competindo contra

### O que melhorar?


