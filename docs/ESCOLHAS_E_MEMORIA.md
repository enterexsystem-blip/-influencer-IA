# Escolhas, rotina simulada e memória de usuários

## Persona
A influencer é uma personagem fictícia: garota branca de São Paulo, divertida, fria com desconhecidos, direta e de poucas palavras. Fala português brasileiro jovem sem exagerar nas gírias.

## Escolhas próprias
A personagem pode escolher responder ou não a cada mensagem. A decisão combina a atividade simulada, se a mensagem tem pergunta, se é uma primeira saudação e sinais de cordialidade. Quando a decisão é `SKIP`, a camada de conversa deve realmente não enviar resposta automática.

Estados simulados disponíveis:
- Em casa, de boa
- Saindo para resolver coisas
- Tomando banho
- Na casa de uma amiga
- Comendo alguma coisa
- Descansando
- Ocupada com coisas pessoais
- Livre e mexendo no celular

Cada estado tem duração aproximada e chance-base de resposta. Isso é roleplay fictício, não rastreamento da localização real da personagem ou do usuário. A atividade deve ser apresentada como parte da vida ficcional dela, não como dado verificado.

## Memória por usuário
`PersonaLifeSystem.rememberUser` armazena um nome de exibição, até 20 fatos curtos, contador de mensagens, horário da última interação e último tópico. `getUserMemory` recupera o perfil e `forgetUser` apaga a memória daquele usuário.

A chave do perfil é derivada por hash do identificador fornecido, para não usar o ID bruto como nome da preferência. Isso não substitui criptografia. Evitar salvar dados sensíveis; oferecer um jeito de apagar a memória.

## Limites importantes da versão atual
- O armazenamento é local ao dispositivo. Não sincroniza entre instalações e não é suficiente para uma influencer pública com muitos usuários.
- A camada de mensagens deve passar um identificador estável do usuário. O texto de uma notificação isolada geralmente não é suficiente para saber com segurança quem falou.
- O app atual apenas lê notificações e mostra a última disponível. A escolha de resposta e a memória estão implementadas como lógica de base, ainda precisam ser conectadas ao canal real de mensagens e ao modelo de IA.
- Para produção, usar backend seguro, autenticação, limites de retenção e consentimento claro para guardar memórias. Não fingir que uma mensagem foi enviada quando a escolha foi não responder.
