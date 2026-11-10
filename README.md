# Contexta Android

Cliente nativo pessoal do [Contexta](https://github.com/leofelipet/contexta). Repo público: **não** coloque URL, token ou keystore aqui. O APK sai vazio; na primeira abertura você cola a URL do **backend Go** e o `API_BEARER_TOKEN`.

Não é o site (`contexta-web`). Colar a URL do Next falha.

## O que faz

- Lista e lê conversas
- Atualiza a lista com o SSE enquanto o app está aberto
- Abas Conversas, Contatos, Tarefas e Busca (as três últimas ainda em breve; o resto em Mais)
- Avisa quando há APK novo no **seu** servidor

## Instalar

O APK é gerado no GitHub Actions (artifact em cada push; Release em tags `v*`). Baixe, instale, abra, cole:

1. URL do Go (`https://seu-servidor`)
2. `API_BEARER_TOKEN`

Assinatura estável (senão o update in-app recusa o APK novo). Crie um keystore **fora do git** e ponha nos secrets do repo:

```sh
keytool -genkeypair -keystore contexta.jks -alias contexta -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 contexta.jks
```

Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`. Guarde o `.jks`. Sem eles o CI ainda compila, mas assina com debug (cada runner = assinatura diferente; update quebra).

## Publicar update

1. Suba `versionCode` / `versionName` em `app/build.gradle.kts`.
2. Tag `v1.0.1` (ou baixe o artifact).
3. No servidor do Go, em `ANDROID_DIST_DIR`:

```text
app.apk        # o APK do Actions
latest.json    # { "version_code": 2, "version_name": "1.0.1", "notes": "..." }
```

`version_code` tem que ser o mesmo `versionCode` do APK. O app chama `GET /api/v1/android/update` no **seu** Go, não o GitHub.
