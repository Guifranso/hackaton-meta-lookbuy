# Respostas para o Formulário de Envio — LookBuy

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

1. A pessoa abre o companion app e toca em **Ativar assistente**. O app inicia um serviço em primeiro plano, com notificação persistente e uso explícito do microfone.
2. O openWakeWord local aguarda “LookBuy”. Após a ativação, o WebRTC VAD aguarda até 5 s pelo início da fala e encerra após 2,5 s de silêncio; o Whisper Tiny transcreve localmente o comando, de até 12 s, antes de acionar a câmera DAT.
3. O `DatCameraClient` captura uma foto; quando uma foto dedicada não estiver disponível, usa o último frame YUV válido da prévia como fallback.
4. Antes do envio ao backend, a `PrivacyFilter` normaliza a rotação e usa ML Kit Face Detection para aplicar blur local de rostos; somente o payload seguro segue para VLM/OCR e consulta de preços.
5. Com identificação de alta confiança, o backend associa marca, produto e embalagem à busca de preço regional.
6. O `ProcessProductLookUseCase` atualiza a interface com nome e preço estimado e o `TextToSpeech` responde em PT-BR: “Encontrei… por aproximadamente…”. Durante o TTS, openWakeWord e WebRTC VAD ficam pausados.
7. O áudio é roteado por HFP/SCO para fones ou óculos Bluetooth; sem acessório, o Android usa microfone e alto-falante do celular. Em uma clarificação, a pessoa responde sem repetir a wake word: há até 8 s de espera e uma repetição da pergunta; depois disso, o assistente volta a aguardar “LookBuy”.

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

Mantemos no app companion as funções de interação, comunicação e apresentação dos resultados, enquanto centralizamos no backend o processamento complexo como análise visual, identificação e verificação de preços.

#### A5[2].b — Por que esse lado *

Óculos e app companion têm limitações para executar essas tarefas localmente com confiabilidade.

#### A5[2].c — O que isso custou *

A consulta de resultados depende de conectividade e pode sofrer latência. Sem conexão, o app informa a indisponibilidade e não apresenta preços não verificados.

#### A5[3].a — A decisão *

Utilizar avaliação multi modal para identificação dos objetos de interesse

#### A5[3].b — Por que esse lado *

Para evitar o uso de modelos VLM muito pesados, e gastos de computação muito elevados com re processamentos.

#### A5[3].c — O que isso custou *

Maior complexidade ao orquestrar as entradas e saídas em paralelo.

#### A5[4].a — A decisão (opcional)

Usamos o último frame da prévia quando `capturePhoto()` falha.

#### A5[4].b — Por que esse lado (opcional)

O MDK separa stream e captura; o fallback preserva a consulta mesmo quando uma foto dedicada não estiver disponível.

#### A5[4].c — O que isso custou (opcional)

O frame pode estar menos nítido que uma foto dedicada. Exibimos a prévia e permitimos repetir a consulta.

#### A5[5].a — A decisão (opcional)

Usamos openWakeWord, WebRTC VAD e Whisper Tiny locais em vez de STT contínuo.

#### A5[5].b — Por que esse lado (opcional)

O openWakeWord mantém escuta leve; WebRTC VAD e Whisper Tiny só processam um comando após “LookBuy”. TTS nativo entrega a resposta com baixa latência.

#### A5[5].c — O que isso custou (opcional)

O modelo aumenta o APK e consome bateria do celular. Mitigamos com WebRTC VAD, limite de 12 s por comando e câmera acionada só após transcrição válida.

## Página 7 — Âncora de originalidade

### A6[1].a — Concorrente 1: nome *

Buscapé

### A6[1].b — Concorrente 1: diferencial *

O Buscapé compara ofertas, histórico e alertas de preço, mas depende de o usuário pesquisar e navegar pela tela. O LookBuy inicia a consulta a partir do item que a pessoa está olhando e devolve as informações por voz, sem interromper o momento da decisão.

### A6[2].a — Concorrente 2: nome *

Google Lens

### A6[2].b — Concorrente 2: diferencial *

O Google Lens realiza pesquisa visual ampla e apresenta resultados em tela. O LookBuy usa a identificação visual como ponto de partida para uma conversa por voz orientada à compra, confirmando a variante quando houver ambiguidade antes de informar preço.

## Página 8 — Mapeamento dos 5 checkpoints obrigatórios

### A7.1 — Uso de IA *

O fluxo usa VLM/OCR para identificar produto, marca e embalagem, além de LLM para gerar e resolver perguntas de clarificação quando houver ambiguidade.

### A7.2 — Câmera ou microfone (canal de entrada) *

HFP/SCO leva o áudio ao celular. openWakeWord, WebRTC VAD e Whisper Tiny transcrevem comandos; a câmera DAT só captura após comando válido.

### A7.3 — Output por áudio *

`TextToSpeech` nativo em PT-BR fala processamento, erro e resultado; pausa openWakeWord/VAD durante a fala. HFP/SCO é priorizado, com fallback no celular.

### A7.4 — Privacidade e dados *

ML Kit borra rostos no celular. A imagem viaja por HTTPS/WSS e é descartada após inferência; logs técnicos sem imagem, áudio ou transcrição ficam por 7 dias.

### A7.5 — Eficiência de bateria *

Óculos fazem captura pontual e áudio. No celular, openWakeWord, WebRTC VAD e Whisper só ativam câmera, rede e IA visual após comando válido; não há streaming contínuo.

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

conseguimos tirar bastante dúvidas sobre o hackaton e obter uma visão geral do que deveriamos entregar, além de obtermos uma visão geral das equipes que estamos competindo contra.

### O que melhorar?

