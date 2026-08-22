# LookBuy — guia do projeto

## O que é

O **LookBuy** é um aplicativo Android hands-free para identificar produtos a partir da câmera e de comandos de voz. A proposta simula a experiência de óculos inteligentes Meta Ray-Ban: o usuário aponta a câmera para um item, pergunta por voz e recebe uma resposta falada e visual com a identificação e o preço estimado.

No desenvolvimento sem os óculos físicos, o projeto usa o **Meta Device Access Toolkit (DAT)** e o **Mock Device Kit (MDK)**. Nesse modo, a câmera traseira do celular representa a câmera dos óculos.

## Tecnologias

| Área | Tecnologia |
|---|---|
| Plataforma | Android, Kotlin e Gradle Kotlin DSL |
| Interface | Jetpack Compose + Material 3 |
| Arquitetura | MVVM com camadas `presentation`, `domain`, `data` e `ai_engine` |
| Dispositivo simulado | Meta DAT 0.8.0 + Mock Device Kit |
| Voz | `SpeechRecognizer` do Android (STT) e `TextToSpeech` (TTS) |
| Voz — arquitetura-alvo | openWakeWord (“LookBuy”) + WebRTC VAD + Whisper Tiny; TTS nativo |
| Privacidade — arquitetura-alvo | ML Kit Face Detection + blur local; HTTPS/WSS; imagem descartada após inferência |
| Assistente em segundo plano | Serviço em primeiro plano ativado explicitamente pelo usuário |
| Visão | `VisionClient` com resposta mockada, pronto para trocar por um VLM real |
| Concorrência | Kotlin Coroutines e Flow/StateFlow |

O projeto usa `minSdk 29`, `targetSdk 35`, Java 11 e Kotlin 2.2.0.

## Estrutura de pastas

```text
mobile/
├── app/                              # Módulo Android principal
│   ├── build.gradle.kts               # Dependências e configurações do app
│   └── src/main/
│       ├── AndroidManifest.xml        # Permissões, activity e credenciais DAT
│       ├── java/com/lookbuy/app/
│       │   ├── LookBuyApplication.kt  # Inicialização única do Meta DAT SDK
│       │   ├── MainActivity.kt        # Permissões Android e ponto de entrada Compose
│       │   ├── ai_engine/             # Voz e visão computacional
│       │   ├── data/                  # Implementações de acesso a dados/API
│       │   ├── domain/                # Regras de negócio, modelos e contratos
│       │   ├── meta_wearables/        # Integração com DAT, MDK, câmera e áudio
│       │   ├── presentation/          # ViewModel e interface Compose
│       │   └── ui/theme/              # Cores, tipografia e tema
│       ├── res/                       # Ícones, textos, tema e XMLs Android
│       ├── test/                      # Testes locais
│       └── androidTest/               # Testes instrumentados
├── docs/                              # Arquitetura, unidades e materiais de referência
├── gradle/libs.versions.toml          # Catálogo central de versões/dependências
├── settings.gradle.kts                # Repositório GitHub Packages do Meta DAT
└── build.gradle.kts                   # Configuração Gradle de nível raiz
```

## Responsabilidade de cada camada

### `presentation/`

- `screens/MainScreen.kt`: tela única do aplicativo. Mostra estados do DAT, prévia da câmera, texto reconhecido, carregamento e resultado da análise.
- `viewmodels/LookBuyViewModel.kt`: orquestra a jornada do usuário. Observa sessão/câmera, inicia voz, recebe fotos ou frames de fallback e chama o caso de uso de análise.

### `meta_wearables/`

- `session/DatSessionManager.kt`: habilita o MDK em builds debug, simula os óculos Ray-Ban Meta, registra o dispositivo e cria/inicia a sessão DAT.
- `camera/DatCameraClient.kt`: cria o stream de vídeo, recebe frames YUV, converte frames para a prévia da interface e solicita a captura de foto.
- `audio/DatAudioClient.kt`: tenta rotear áudio para Bluetooth HFP/SCO. Sem fone/óculos Bluetooth, o Android usa o microfone e alto-falante do próprio celular.

### `ai_engine/`

- `speech/SttClient.kt`: transforma a fala em texto usando o reconhecedor de voz do Android.
- `speech/TtsClient.kt`: fala mensagens de processamento e o resultado para o usuário.
- `vision/VisionClient.kt`: ponto de integração com visão. Hoje retorna um resultado mockado após 2 segundos; pode ser substituído por Gemini, OpenAI ou backend próprio.
- `vision/PrivacyFilter.kt`: ponto de integração do ML Kit Face Detection e blur local de rostos antes do envio.

### `domain/`

- `models/ProductModels.kt`: modelos de produto e preço.
- `repository/`: contratos das fontes de dados.
- `usecases/ProcessProductLookUseCase.kt`: fluxo de negócio principal: fala “processando”, chama a visão e fala o resultado.
- `usecases/HandleBackendResponseUseCase.kt`: tratamento de respostas de backend.

### `data/`

- `remote/ApiService.kt`: contrato para chamadas remotas.
- `repository/*Impl.kt`: implementações dos repositórios de preço e backend.

## Fluxo do aplicativo

```text
App abre
  → solicita CAMERA, RECORD_AUDIO e BLUETOOTH_CONNECT
  → inicializa Meta DAT
  → em debug, habilita MDK e configura a câmera traseira do celular
  → registra e inicia uma DeviceSession
  → inicia Stream de vídeo DAT
  → converte frames YUV para a prévia da tela

Usuário ativa o assistente no celular
  → serviço em primeiro plano mantém o app elegível para áudio com a tela apagada
  → no desenho final, openWakeWord com modelo personalizado aguarda "LookBuy"
  → após a ativação, WebRTC VAD aguarda até 5 s pelo início da fala e encerra após 2,5 s de silêncio
  → Whisper Tiny transcreve localmente comandos de até 12 s
  → no MVP atual, o usuário segura o microfone e fala
  → STT transforma voz em texto
  → tenta capturar foto DAT
  → se o MDK não fornecer uma foto, usa o último frame válido da prévia
  → ProcessProductLookUseCase chama VisionClient
  → interface mostra o resultado e TTS o reproduz
```

## Câmera, MDK e fallback

O MDK possui duas fontes de mídia independentes:

1. **Vídeo de streaming**: alimenta a prévia e é configurado como `CameraFacing.BACK`, ou seja, a câmera traseira do celular.
2. **Imagem de captura**: é a imagem retornada por `capturePhoto()`. Em testes com MDK ela pode não estar configurada.

Por isso o projeto tem um fallback: se `capturePhoto()` falhar, o último frame que aparece na prévia é enviado para a análise. Dessa forma, a demonstração continua funcional usando apenas a câmera do celular.

## Áudio HFP

O cartão “Áudio HFP” fica vermelho quando não há um dispositivo Bluetooth de chamadas conectado. Isso **não impede** o app de reconhecer ou falar pelo celular.

- Com fone/óculos Bluetooth compatível com HFP/SCO: áudio é roteado para o acessório.
- Sem acessório: fala e reprodução usam o microfone e alto-falante do celular.
- O Mock Device Kit não simula áudio Bluetooth.
- Na arquitetura-alvo, openWakeWord e WebRTC VAD ficam pausados durante a fala do TTS e voltam após seu término, evitando que o app reconheça a própria resposta.

## Privacidade e logs — arquitetura-alvo

A imagem passa por blur local de rostos antes de trafegar por HTTPS/WSS. O backend a processa em memória e a descarta após a inferência; não retém imagem, áudio, transcrição ou localização precisa. Logs técnicos de data/hora, status, latência e erro ficam por até 7 dias, sem conteúdo sensível.

## Permissões

Declaradas no `AndroidManifest.xml` e solicitadas em runtime por `MainActivity.kt`:

- `CAMERA`: câmera traseira usada pelo MDK no celular.
- `RECORD_AUDIO`: reconhecimento de fala.
- `BLUETOOTH_CONNECT`: comunicação/roteamento Bluetooth em Android 12 ou superior.
- `INTERNET`: reservado para futuras integrações de IA e preços.
- `BLUETOOTH`: compatibilidade com versões mais antigas do Android.

## Como executar

1. Abra a pasta `mobile` no Android Studio.
2. Garanta que o dispositivo físico tenha permissão de câmera e microfone para o LookBuy.
3. Execute a variante `debug` em um celular Android.
4. Espere os cartões de Registro Meta AI, Sessão DAT e Câmera DAT ficarem ativos.
5. Verifique a prévia: ela deve mostrar a câmera traseira do celular.
6. Toque em **Ativar assistente** para iniciar o serviço de áudio. No MVP, segure o botão de microfone, faça uma pergunta sobre o objeto enquadrado e solte.

Pelo terminal PowerShell, o APK debug pode ser gerado com:

```powershell
.\gradlew.bat :app:packageDebug
```

O APK é produzido em `app/build/outputs/apk/debug/app-debug.apk`.

## Configuração de dependências

As versões estão centralizadas em `gradle/libs.versions.toml`. O SDK da Meta é obtido do GitHub Packages, configurado em `settings.gradle.kts`.

Para ambientes que precisem baixar novamente as dependências Meta, configure um token do GitHub com acesso ao repositório do DAT em uma destas formas:

```properties
# local.properties (não versionar)
github_token=SEU_TOKEN
```

ou pela variável de ambiente `GITHUB_TOKEN`.

## Situação atual e próximos passos

O app já demonstra o fluxo completo de interface, câmera simulada, fala, resposta e resultado visual. A análise de produto ainda é mockada; a ativação explícita cria um serviço em primeiro plano, mas openWakeWord, WebRTC VAD, Whisper Tiny e ML Kit Face Detection ainda não foram integrados. Para produção, os próximos passos são:

1. Substituir `VisionClient` por uma API/VLM real.
2. Implementar consulta de preços nos repositórios de `data/`.
3. Proteger chaves de API no backend, nunca dentro do APK.
4. Configurar credenciais reais do Meta Developer Portal para builds de produção.
5. Testar com óculos Meta reais e um dispositivo HFP conectado.
6. Integrar openWakeWord com modelo personalizado “LookBuy”, WebRTC VAD e Whisper Tiny no serviço para substituir o push-to-talk, com 5 s para início da fala, 2,5 s de silêncio e comando máximo de 12 s.
7. Integrar ML Kit Face Detection ao `PrivacyFilter` e aplicar blur antes do upload.

## Materiais de apoio

- `docs/LookBuy_Arquitetura.md`: arquitetura detalhada e referências do DAT.
- `docs/Unidade 12.pdf` e `docs/Unidade 13.pdf`: materiais de estudo.
- `docs/Notebook_Codigo_AKCIT_Camp.ipynb`: anotações e exemplos técnicos.
- `docs/Lookbuy (1).pdf` e `docs/Edital AI Glasses Brasil 2026.pdf`: contexto do desafio e proposta.
