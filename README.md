# Lectio

App Android para a oração diária: momento da manhã, leituras da missa, homilias do dia e reflexão.

- **Manhã:** roteiro de 4 minutos gerado pelo [Magisterium AI](https://www.magisterium.com) a partir das leituras, com um gesto concreto para viver no dia.
- **Leituras:** liturgia diária em português ([liturgia.up.railway.app](https://liturgia.up.railway.app)).
- **Homilias:** vídeos de homilia do dia no YouTube.
- **Reflexão:** "As palavras dos Papas" do [Vatican News](https://www.vaticannews.va/pt/palavra-do-dia.html) e a oração do dia.
- **Aviso diário** no horário escolhido.

## Instalar

Baixe o APK da [última versão](../../releases/latest) e abra no celular. As próximas versões aparecem dentro do app, com o botão **Atualizar**.

A aba Manhã precisa de uma chave gratuita da API do Magisterium, criada no [Console da API](https://www.magisterium.com/pt/developers/api) e colada no próprio app.

## Publicar uma versão nova

```powershell
.\publicar.ps1 -Versao 1.2 -Notas "O que mudou"
```

Requer a chave de assinatura em `assinatura/` (fora do git). Toda versão precisa ser assinada com a mesma chave.
