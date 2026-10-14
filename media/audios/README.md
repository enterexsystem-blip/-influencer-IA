# Biblioteca de áudio da Ayla

## Estado atual

Os áudios enviados estão preservados em `01-cumprimentos/`, com os nomes originais e sem extensão de arquivo. Não foram renomeados nem apagados. O arquivo `-influencer-IA-main.zip` é um ZIP do projeto inteiro, não um pacote exclusivo de áudio; o seletor de gravações ignora esse ZIP.

## Catálogo

`catalog.json` indexa os arquivos encontrados no repositório, com tamanho, hash, caminho atual e uma categoria sugerida a partir do nome. As transcrições sugeridas são apenas pistas do nome do arquivo — ainda não foram confirmadas ouvindo cada áudio. Arquivos com o mesmo hash são cópias binariamente idênticas.

## Categorias sugeridas

- `01-cumprimentos/` — saudações.
- `02-respostas-curtas/` — respostas breves e reações.
- `03-emocoes/` — falas marcadas com emoções.
- `04-ausencias-desculpas/` — demora, banho, sono e explicações.
- `05-encerrar-conversa/` — despedidas e bloqueio.
- `06-exclusivos-kyodai/` — falas direcionadas ao criador e conteúdo íntimo.

As categorias no catálogo são sugestões de organização; os arquivos físicos permanecem onde estão para não quebrar os caminhos. O backend agora localiza gravações compatíveis pelo nome real, em vez de exigir nomes fixos como `oi_animado.mp3`.

## Segurança e preservação

Não apague os arquivos-fonte até conferir as gravações e validar o funcionamento no Telegram. O backend exclui gravações íntimas da seleção automática comum. Antes de liberar áudios exclusivos, valide no código que só o destinatário autorizado possa recebê-los.
