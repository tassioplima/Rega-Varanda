# Rega Varanda 🪴

App Android (Kotlin + Jetpack Compose) para ajudar a cuidar das plantas da varanda:
ele estima quanto sol a varanda recebe com base na sua localização e na orientação
da fachada, calcula a frequência de rega ideal para cada planta considerando o
clima do dia, e dá dicas diárias de cuidado.

## Como funciona

1. **Localização e clima**: o app pede permissão de localização e usa a API pública
   e gratuita [Open-Meteo](https://open-meteo.com) (sem necessidade de API key) para
   buscar temperatura máxima/mínima, índice UV, chance de chuva e a duração real de
   sol (já descontando nuvens) do dia.
2. **Orientação da varanda**: em *Configurações* você escolhe para qual direção a
   varanda/frente da casa está voltada (Norte, Sul, Leste, Oeste, etc.) — isso define
   quanto da luz do dia aquela fachada recebe. Se você já souber quantas horas de sol
   direto sua varanda recebe, pode informar manualmente para uma estimativa mais precisa.
3. **Plantas**: cada planta tem uma categoria (cacto/suculenta, hortaliça, ervas, flor
   ornamental, folhagem tropical, outra) com uma frequência de rega e necessidade de
   adubação padrão, que você pode sobrescrever.
4. **Cálculo de rega**: combinando o sol estimado com a temperatura e chance de chuva
   do dia, o app decide se a planta precisa de rega 1x, 2x (dias muito quentes e
   ensolarados) ou se pode esticar o intervalo (previsão de chuva).
5. **Dicas diárias**: um card com recomendações do dia (calor extremo, UV alto,
   necessidade de adubação, dica de cuidado da planta).
6. **Lembretes**: notificações automáticas (via WorkManager) checam 2x por dia
   (manhã e fim de tarde) se alguma planta está precisando de rega — e o texto muda
   quando o sol está forte e é preciso regar de novo no mesmo dia.
7. **Fotos e evolução**: na tela de cada planta você tira fotos (câmera do celular)
   que ficam guardadas num histórico. Cada foto é analisada pela IA de visão escolhida
   (Anthropic Claude ou Google Gemini), que sugere um estado de saúde (Saudável/
   Atenção/Crítica) e dá diagnóstico + dicas específicas de rega, poda, vitaminas/
   adubação e troca de vaso. Você confirma ou corrige o estado sugerido.
8. **Dashboard de saúde**: uma tela separada (ícone de coração) com o resumo de todas
   as plantas — estado atual, miniatura da última foto e tendência (melhorando/
   piorando/estável) com base no histórico de avaliações.
9. **Bússola embutida**: em Configurações (ou ao editar uma planta com local próprio)
   há um botão "Usar bússola" que lê o magnetômetro do celular em tempo real e já
   seleciona a direção mais próxima automaticamente — não depende de nenhum app de
   bússola externo (o Android não vem com um por padrão).
10. **Local por planta**: ao criar/editar uma planta você pode marcar que ela está em
    um local diferente (ex.: fundos da casa) e escolher uma orientação/estimativa de
    sol específica para ela. Sem isso, a planta usa a orientação padrão da varanda.

## Configurando a análise de fotos por IA

Na tela de Configurações, escolha o provedor (Anthropic ou Google Gemini) e cole a
chave da API correspondente:
- Anthropic: gerada em console.anthropic.com
- Google Gemini: gerada em aistudio.google.com/apikey

Cada chave é guardada separadamente e de forma criptografada (EncryptedSharedPreferences)
só no aparelho, e usada apenas para chamar a API do provedor escolhido diretamente do
celular — nunca passa por nenhum servidor intermediário. Sem chave configurada, as
fotos continuam sendo guardadas normalmente, só a análise de IA fica pendente.

## Stack técnica

- Kotlin + Jetpack Compose (Material 3), navegação sem animação de transição
- Navigation Compose
- Room (persistência das plantas e fotos)
- DataStore Preferences (configuração da varanda) + EncryptedSharedPreferences (chave de API)
- OkHttp + org.json (cliente HTTP simples para Open-Meteo e para a API da Anthropic)
- Coil (carregamento das fotos das plantas)
- WorkManager (lembretes periódicos)
- `android.location.LocationManager` puro (sem depender do Google Play Services)
- Captura de fotos via `ActivityResultContracts.TakePicture()` + FileProvider (sem CameraX)
- Bússola via sensor `TYPE_ROTATION_VECTOR` (`SensorManager`), sem app externo

## Como abrir e rodar

1. Abra a pasta `RegaVaranda` no Android Studio (versão recente, com JDK 17+).
2. Deixe o Gradle sincronizar (usa Gradle 8.13 + AGP 8.9.1 + Kotlin 2.1.0).
3. Rode em um emulador ou celular com Android 8.0 (API 26) ou superior.

Ou via linha de comando:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Este projeto já foi validado com builds reais (`assembleDebug`) e instalado/testado
> em um dispositivo Android físico durante o desenvolvimento, incluindo captura de
> foto pela câmera real, leitura real do sensor de bússola e navegação entre todas
> as telas.

## Possíveis próximos passos

- Ícone do app é um placeholder simples (vetor de folha); pode ser substituído por uma
  arte final.
- Histórico/gráfico de regas ao longo do tempo.
- Ajuste fino dos fatores de orientação/sol para o hemisfério sul, caso necessário.
- Migração de banco de dados real caso o app já esteja em uso (hoje usa
  `fallbackToDestructiveMigration`, adequado só em desenvolvimento).
