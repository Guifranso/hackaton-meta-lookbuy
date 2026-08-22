# Formulário de Envio

O formulário está dividido em quatro partes:

- **Seção A — Documento estruturado:** problema, usuário-alvo, walkthrough de uso, decisões técnicas, concorrentes e os cinco pilares técnicos obrigatórios (IA, câmera/microfone, áudio, privacidade e bateria).
- **Seção B — Diagrama de arquitetura:** imagem do diagrama e código-fonte Mermaid.
- **Seção C — Vídeo-pitch:** link de um vídeo de 2 a 3 minutos hospedado externamente.
- **Seção D — Confirmações finais:** manutenção do escopo, coerência entre artefatos e autoria/uso de IA.

> **Observação:** use a seção D para explicar qualquer alteração no projeto ou escopo desde a inscrição.

## Página 1 — Identificação

### E-mail *

### Nome da equipe *

Selecione o nome da sua equipe de acordo com a inscrição.

### Trilha temática *

Selecione a trilha declarada na inscrição.

### Título da solução *

Um nome curto e memorável para a sua solução. **Máximo: 100 caracteres.**

### Resumo em uma frase *

Descreva a solução em uma única frase: **o que faz e para quem**. **Máximo: 160 caracteres.**

## Página 2 — Seção A: Documento estruturado

O núcleo da entrega.

### A1 — O problema *

Que dor específica vocês resolvem? Quem sente essa dor e em que momento? _Evitem frases de efeito; descrevam a situação real._ **Máximo: 400 caracteres.**

### A2 — Usuário-alvo *

Descrevam a pessoa concreta que usará a solução (não “todo mundo”): idade, contexto, limitação e frequência de uso. **Máximo: 300 caracteres.**

### A3 — Walkthrough de interação (caminho principal) *

Descrevam **um** caso de uso real, turno a turno, em **5 a 9 passos numerados**.

Para cada passo, informem: o que o usuário faz ou diz → qual canal dos óculos é acionado (câmera/microfone) → onde a IA entra → o que é respondido por áudio.

Citem tecnologias reais. **Mínimo: 5 passos numerados.**

### A4 — Walkthrough de exceção (quando dá errado) *

Escolham um cenário em que a solução falha e descrevam três momentos: como o sistema **percebe** que deu errado, como ele **reage** e como o usuário **fica sabendo**. Nos óculos, a única forma de aviso é por áudio, sem tela para apresentar erros — exceto a tela do celular, que o usuário pode não estar olhando.

Em um dispositivo sem display, o pior erro é o silencioso: entender errado e responder com confiança. Mostrem como a solução evita isso.

_Cenários possíveis: a câmera não consegue ler o que foi apontado; não há internet para consultar a IA; a sessão cai (óculos dobrado, Bluetooth desconecta, app em segundo plano); ou o pedido do usuário é ambíguo._

**Máximo: 400 caracteres.**

### A5 — Decisões técnicas e trade-offs

Todo projeto real envolve escolhas em que ganhar de um lado custa do outro. Listem de **3 a 5 decisões** desse tipo; não apenas ferramentas adotadas, mas bifurcações em que havia mais de um caminho razoável.

Foquem nas decisões que mais afetam a viabilidade nos AI glasses: onde a IA roda (celular vs. nuvem), como lidam com o limite de captura da câmera, o que fazem quando a sessão cai, como contornam a ausência de áudio no SDK e como equilibram latência e bateria.

> **Observação:** as decisões 1 a 3 são obrigatórias. As decisões 4 e 5 são opcionais e só devem ser preenchidas se forem reais; a quantidade não interfere na nota.

#### Estrutura para cada decisão

- **A5[n].a — A decisão:** em até **80 caracteres**, descrevam a escolha no formato “Fizemos X em vez de Y”. O “em vez de Y” é obrigatório.
- **A5[n].b — Por que esse lado:** em até **160 caracteres**, expliquem o ganho ligado a uma restrição real (prazo, SDK, bateria, latência ou hardware). Evitem “porque é melhor”; digam melhor _em quê_.
- **A5[n].c — O que isso custou:** em até **160 caracteres**, expliquem a renúncia ou risco e como pretendem mitigar.

_Exemplos: “Fizemos OCR na nuvem em vez de local”; “Fizemos captura de foto sob demanda em vez de stream contínuo”._

_Exemplo de justificativa: “O modelo de OCR não roda no dispositivo dentro do prazo proposto de 5 segundos; a nuvem entrega precisão sem depender de otimização que não teríamos tempo de fazer.”_

_Exemplo de custo: “Custa dependência de internet e cerca de 1–2 s de latência. Mitigamos com cache das últimas consultas e mensagem clara quando offline.”_

#### Decisão 1 (obrigatória)

##### A5[1].a — A decisão *

##### A5[1].b — Por que esse lado *

##### A5[1].c — O que isso custou *

## Página 3 — Decisão técnica e trade-off 2 (obrigatória)

### A5[2].a — A decisão *

### A5[2].b — Por que esse lado *

### A5[2].c — O que isso custou *

## Página 4 — Decisão técnica e trade-off 3 (obrigatória)

### A5[3].a — A decisão *

### A5[3].b — Por que esse lado *

### A5[3].c — O que isso custou *

## Página 5 — Decisão técnica e trade-off 4 (opcional)

### A5[4].a — A decisão

### A5[4].b — Por que esse lado

### A5[4].c — O que isso custou

## Página 6 — Decisão técnica e trade-off 5 (opcional)

### A5[5].a — A decisão

### A5[5].b — Por que esse lado

### A5[5].c — O que isso custou

## Página 7 — Âncora de originalidade

### A6 — Concorrentes e diferenciais

Citem duas soluções ou produtos existentes que fazem algo parecido. Para cada um, digam em uma frase o que a solução de vocês faz de diferente.

> **Atenção:** a Meta AI nativa também conta como concorrente. Os óculos já vêm com Meta AI embarcada, que responde a comandos de voz, descreve o que a câmera vê, traduz e responde perguntas. Pesquisem se ela já realiza o que vocês propõem. Se realizar, a solução precisa fazer melhor, de modo diferente ou em um contexto que ela não cobre.

“Concorrente” não precisa ser uma empresa: pode ser o próprio assistente da Meta disponível no dispositivo.

#### A6[1].a — Concorrente 1: nome *

Nome de um concorrente ou solução similar existente. Caso não exista, escrevam “Não há” ou “Não temos”. **Máximo: 80 caracteres.**

#### A6[1].b — Concorrente 1: diferencial *

O que a solução de vocês faz de diferente desse concorrente. **Máximo: 160 caracteres.**

#### A6[2].a — Concorrente 2: nome *

Nome de outro concorrente ou solução similar existente. Caso não exista, escrevam “Não há” ou “Não temos”. **Máximo: 80 caracteres.**

#### A6[2].b — Concorrente 2: diferencial *

O que a solução de vocês faz de diferente desse concorrente. **Máximo: 160 caracteres.**

## Página 8 — Mapeamento dos 5 checkpoints obrigatórios

### A7 — Checkpoints técnicos

Para cada checkpoint técnico do _Edital (Seção 8.1)_, expliquem como a solução o cumpre.

#### A7.1 — Uso de IA *

Qual componente de IA vocês usam e como isso é funcional e comprovável. **Máximo: 200 caracteres.**

#### A7.2 — Câmera ou microfone (canal de entrada) *

Como a câmera ou o microfone é o canal principal de entrada. **Máximo: 200 caracteres.**

#### A7.3 — Output por áudio *

Como a resposta chega ao usuário em áudio. **Máximo: 200 caracteres.**

#### A7.4 — Privacidade e dados *

Como vocês tratam os dados coletados. **Máximo: 200 caracteres.**

#### A7.5 — Eficiência de bateria *

Qual é a estratégia de economia de energia. **Máximo: 200 caracteres.**

## Página 9 — Seção B: Diagrama de arquitetura

Entreguem os dois campos: imagem renderizada e código-fonte.

### B1 — Imagem do diagrama *

Envie a imagem exportada e renderizada do diagrama, usando [Mermaid Live](https://mermaid.live/) ou equivalente.

Formatos aceitos: PDF, PNG, JPG ou SVG. Tamanho máximo: **100 MB**.

### B2 — Código Mermaid *

Envie o código-fonte que gera o diagrama acima em DOCX, TXT ou PDF. Ele deve começar com um tipo de diagrama [Mermaid](https://mermaid.js.org/).

**Requisitos:** conter ao menos um nó por checkpoint, tecnologias/APIs nomeadas nos nós e o fluxo direcionado com setas.

Formatos aceitos: PDF ou documento. Tamanho máximo: **100 MB**.

## Página 10 — Seção C: Vídeo-pitch

Garanta que o link esteja acessível até o fim da avaliação.

### C1 — Vídeo-pitch elevator *

Cole o link do vídeo (YouTube não listado, Google Drive com acesso, Vimeo etc.). Garanta que ele esteja acessível para avaliação.

Formato aceito: vídeo. Tamanho máximo: **1 GB**.

### C2 — Duração do vídeo *

Confirme que o vídeo respeita o limite de 2 a 3 minutos:

- [ ] Menos de 2 min
- [ ] Entre 2 e 3 min
- [ ] Mais de 3 min

## Página 11 — Avaliação do Ideathon

Sua opinião sobre o Ideathon leva menos de um minuto e ajuda a melhorar as próximas edições. As respostas são usadas de forma agregada.

### De 0 a 10, o quanto o Ideathon de hoje foi útil para a equipe? *

`0` — Quase nada · `1` · `2` · `3` · `4` · `5` · `6` · `7` · `8` · `9` · `10` — Muito útil

### O que foi mais útil?

O que mais ajudou? Pode ser uma palestra, um momento de mentoria, uma explicação específica ou algo que vocês levarão para a entrega.

### O que melhorar?

O que faltou, o que confundiu ou o que vocês mudariam no Ideathon? Quanto mais específico, melhor será possível melhorar.

## Página 12 — Confirmações finais

### Coerência entre artefatos *

- [ ] Confirmamos que o documento, o diagrama e o vídeo descrevem a mesma solução de forma coerente.

### Autoria e uso de IA *

- [ ] Confirmamos que as decisões técnicas e o conteúdo são da equipe. Ferramentas de IA podem ter apoiado a redação, mas o conteúdo reflete nosso trabalho.

### Escopo mantido *

- [ ] Confirmamos que esta entrega mantém o escopo do projeto ou ideia submetido na inscrição (_Edital_, Seção 14.1).
- [ ] Mudamos o escopo do projeto ou ideia submetido. Neste caso, expliquem as alterações na seção seguinte do formulário.
