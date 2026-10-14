# Biblioteca de áudio da Ayla

## Categorias
- `01-cumprimentos/` — 7 variações de “oi”, com emoções e entonações diferentes.
- `02-respostas-curtas/` — respostas breves sobre como ela está.
- `03-emocoes/` — falas ligadas a sentimentos.
- `04-ausencias-desculpas/` — explicações de rotina e demora para responder.
- `05-encerrar-conversa/` — reservado para despedidas e encerramentos.
- `06-exclusivos-kyodai/` — reservado para falas exclusivas do criador.

## Catálogo
O arquivo `catalog.json` registra a transcrição/ intenção e o caminho de destino para cada um dos 12 áudios únicos recebidos até agora. O mesmo áudio “Estou bem, obrigada” foi enviado novamente; foi catalogado uma única vez.

## Importante: arquivos de áudio
Os nomes e conteúdos identificáveis foram catalogados e organizados por pasta de destino. Os MP3 originais ainda estão anexados à conversa, não dentro deste repositório. O conector atual do GitHub só permite criar/editar arquivos de texto, então não consigo afirmar que os binários foram enviados. Para a Ayla tocar esses áudios, os MP3 precisam ser copiados para os caminhos listados no catálogo (ou para um armazenamento de mídia acessível ao backend).

## Convenção
Use nomes em minúsculas, sem espaços ou acentos. Registre transcrição, intenção, emoção, tags e destinatário permitido (`todos` ou `kyodai`). A regra de destinatário também deve ser validada no código, não apenas pela pasta.
