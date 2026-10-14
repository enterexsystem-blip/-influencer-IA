const BANKS = [
  { test: /\b(oi+|olá|ola|eae|eai|salve|opa|bom dia|boa tarde|boa noite)\b/i, replies: [
    "ooii 💗 como cê tá hoje?", "oii, apareceu por aqui hehe. Como foi teu dia?", "oiee, tudo tranquilo contigo?", "eae mn, qual foi a boa de hoje?", "oii 😊 me conta o que tá acontecendo", "opa, chegou de boa? Como cê tá?", "ooi, tava fazendo o quê agora?", "eae! Espero que teu dia esteja sendo leve", "oii, me atualiza das novidades", "oiee 💕 como andam as coisas?"
  ]},
  { test: /\b(tudo bem|tudo certo|suave|blz|como você tá|como cê tá|como tu tá)\b/i, replies: [
    "tô bem simm, obrigada por perguntar 💗 e tu?", "tudo certinho por aqui 😊 como tá teu lado?", "tô tranquila. E tu, como tá de verdade?", "tudo na paz hehe. Teu dia tá tranquilo?", "tô de boa, mn. E contigo?", "por aqui tá tudo certo. Como tu tá se sentindo?", "tô bemm 💕 e aí, alguma novidade?", "tudo tranquilo por enquanto. E por aí?"
  ]},
  { test: /\b(triste|deprê|mal hoje|tô mal|to mal|péssimo|pessima|ansioso|ansiosa|sozinho|sozinha|desanimado|desanimada)\b/i, replies: [
    "poxa, sinto muito que tu esteja passando por isso 🥺 quer me contar o que rolou?", "ahh, queria que as coisas estivessem mais leves pra ti. Quer conversar?", "tô te ouvindo, viu? Pode falar no teu tempo", "não precisa fingir que tá tudo bem comigo. O que aconteceu?", "puts… imagino que não esteja fácil. Quer desabafar um pouco?", "sinto muito, mn. Foi alguma coisa que aconteceu hoje?", "quer me explicar o que tá pesando? Vou te acompanhar com calma", "poxa 💗 se quiser colocar pra fora, pode falar comigo"
  ]},
  { test: /\b(feliz|consegui|deu certo|vitória|vitoria|passei|aprovado|conquistei|realizei)\b/i, replies: [
    "aí simmm 🥹💗 fiquei feliz por ti! Como aconteceu?", "boaaa, sabia que tu tinha uma notícia boa pra contar hehe", "que massa! Tu tava esperando por isso há muito tempo?", "olha só 👀 me conta tudo, quero entender", "issooo! Deve dar um alívio enorme, né?", "amei saber disso 💕 qual foi a melhor parte?", "pô, notícia boa assim merece comemoração", "que bom, de verdade. Como tu tá se sentindo agora?"
  ]},
  { test: /\b(trabalh|emprego|chefe|serviço|servico|corre|cansad[oa]|exaust[oa])\b/i, replies: [
    "eita, o corre tá puxado hoje? Conseguiu fazer uma pausa?", "imagino que canse. O que mais te tomou tempo hoje?", "força aí, viu? Espero que tu consiga descansar depois", "mn, trabalho pesado acaba com a energia mesmo. Já comeu alguma coisa?", "como foi teu dia no serviço, aconteceu algo diferente?", "tu tá conseguindo levar tudo de boa ou tá acumulando cansaço?", "tomara que o resto do dia passe mais tranquilo", "me conta: foi correria normal ou aconteceu alguma coisa?"
  ]},
  { test: /\b(comer|comida|fome|almoç|almoc|jantar|lanche|café da manhã|cafe da manha)\b/i, replies: [
    "ihh, bateu a fome 😅 já sabe o que vai comer?", "o que tu tá pensando em comer hoje?", "não esquece de se cuidar também, viu? Conseguiu fazer uma refeição?", "agora fiquei curiosa: qual comida tu mais gosta?", "se tivesse qualquer coisa na tua frente agora, o que escolheria?", "já comeu ou tá deixando pra depois de novo? 👀", "hmm, fala uma comida que tu nunca enjoa", "tomara que tu consiga comer algo gostoso hoje 💗"
  ]},
  { test: /\b(dormir|sono|boa noite|vou deitar|descansar|acordad[oa])\b/i, replies: [
    "então descansa bem 💗 amanhã a gente continua", "vai cuidar desse sono, mn. Boa noitee 🌙", "dorme bemm, espero que tu acorde mais leve", "boa noite 😊 deixa as preocupações descansarem um pouquinho também", "vai repousar, tu merece um descanso de verdade", "até depois, viu? Se cuida 💕", "descansa direitinho. Não precisa resolver tudo hoje", "boa noiteee, fica bem 🌙"
  ]},
  { test: /\b(obrigad[oa]|valeu|agradeço|agradeco|tmj|tamo junto)\b/i, replies: [
    "imaginaaa 💗", "por nada, mn. Tamo junto", "que isso, nem precisa agradecer 😊", "disponha, viu?", "magina hehe, fico feliz em ajudar", "por nadinhaa ✨", "sempre que eu puder te dar uma força, vou tentar", "tranquilo 💕"
  ]},
  { test: /\b(desculpa|foi mal|perdão|perdao|vacilei|errei)\b/i, replies: [
    "tá tudo bem, vamos conversar com calma 💗", "eu entendi. O importante é a gente se entender", "não precisa se desesperar. Me conta o que aconteceu", "beleza, obrigada por falar comigo sobre isso", "tudo bem, acontece. Como tu prefere resolver?", "tô te ouvindo, sem julgamento", "vamos por partes, tá? O que aconteceu de verdade?", "entendi. Quero compreender teu lado também"
  ]},
  { test: /\b(gosto de você|gosto de vc|te adoro|te amo|saudade|senti sua falta|senti tua falta)\b/i, replies: [
    "awn, que fofo tu falar isso 💗", "olhaaa, assim tu me deixa tímida hehe", "que carinho 🥹 como foi teu dia?", "hmm, fiquei sem jeito agora 😳", "tu é um querido às vezes, sabia?", "awn 💕 conta, o que te fez lembrar de mim?", "hehe, que mensagem bonitinha", "eu gosto das nossas conversas. Me conta como tu tá"
  ]},
  { test: /\b(brig|discut|raiva|irritad[oa]|ódio|odio|estressad[oa])\b/i, replies: [
    "eita, parece que isso te tirou do sério. O que aconteceu?", "quer me contar a situação antes de tirar uma conclusão?", "respira um pouquinho, mn. Qual foi a parte que mais te irritou?", "puts… foi alguém que fez isso contigo?", "entendi. Tu quer desabafar ou pensar numa solução?", "isso parece ter mexido contigo. Me explica melhor", "não vou presumir quem tá certo. Conta teu lado", "tá, vamos entender isso sem pressa"
  ]},
  { test: /\b(música|musica|funk|som|cant|letra|playlist)\b/i, replies: [
    "qual som tá tocando aí agora? 👀", "me indica uma música boa então hehe", "tu curte mais funk ou escuta de tudo?", "agora quero saber: qual música combina com teu momento?", "tem alguma letra que ficou na tua cabeça esses dias?", "manda o nome da música que tu tá ouvindo", "qual artista tu mais escuta ultimamente?", "se fosse escolher um som pra repetir o dia todo, qual seria?"
  ]},
  { test: /\b(foto|imagem|selfie|vídeo|video|áudio|audio|manda|envia)\b/i, replies: [
    "consigo escolher uma mídia quando houver um arquivo correspondente na minha biblioteca 💗", "tô pronta pra usar a biblioteca de mídia; preciso que os arquivos estejam nas pastas certas primeiro", "se tu pediu uma foto, vídeo ou áudio específico, o arquivo precisa estar cadastrado na biblioteca", "quero te mandar a mídia certa, não um arquivo aleatório. Qual tipo tu quer?", "a biblioteca fica separada por fotos, vídeos e áudios. Quando tiver arquivos disponíveis, dá pra selecionar por categoria"
  ]},
  { test: /\b(quem é você|quem e voce|quem é vc|quem e vc|seu nome|teu nome)\b/i, replies: [
    "sou a Ayla 💗 prazer hehe", "Ayla por aqui 😊 e como posso te chamar?", "me chamo Ayla. Me conta um pouco de ti", "sou a Ayla, uma influencer fictícia de São Paulo ✨", "Ayla! hehe, agora me fala de ti", "meu nome é Ayla 💕 e tu, como prefere ser chamado?"
  ]},
  { test: /\b(não sei|nao sei|sei lá|sei la|tanto faz|confuso|confusa|decidir)\b/i, replies: [
    "tá tudo bem ainda não saber. Qual opção parece menos ruim?", "vamos pensar junto, sem precisar decidir agora", "me conta o que tá te deixando em dúvida", "se a gente separar isso em partes, fica mais fácil. Qual é o principal problema?", "não precisa ter a resposta na hora, viu?", "o que tu já sabe com certeza sobre essa situação?", "vamos com calma. O que tu queria que acontecesse?", "hmm, qual é a parte mais difícil de escolher?"
  ]},
  { test: /\b(kkkk+|rsrs+|hahaha+|😂|🤣)\b/i, replies: [
    "KKKKKK paraa 😂", "ai mn KKKKK tu não existe", "mds KKKKKK 😭", "KKKKKK aí tu me quebra", "não tankei KKKKK", "calma aí KKKKK 😂", "tu é figura, sério KKKK", "eu não tava pronta pra essa KKKKK"
  ]}
];

const GENERIC = [
  "hmm, entendi. O que aconteceu depois?", "sério? Me conta mais um pouquinho 👀", "quero entender direito. Qual foi a parte principal?", "ahh, saquei. E como tu ficou com isso?", "tô acompanhando, continua", "eita, não esperava essa parte. Como começou?", "me explica só mais um detalhe pra eu não entender errado", "entendi teu ponto. O que tu pensa em fazer agora?", "pera, quero ter certeza de que entendi: como foi?", "hmm, e o que aconteceu antes disso?", "certo, tô te ouvindo. Continua", "isso parece importante pra ti. Por quê?", "pô, quero entender teu lado sem inventar nada", "me conta o resto quando puder", "e como essa história terminou?", "o que mais tu acha importante eu saber?", "tá, vou te acompanhar. Qual é o próximo detalhe?", "entendi mais ou menos; me ajuda com um contexto?", "como tu chegou nessa conclusão?", "e o que tu gostaria que acontecesse?", "essa parte me deixou curiosa. Como foi exatamente?", "hum, faz sentido dentro do que tu contou", "me conta um pouco mais sobre isso", "não quero presumir coisa que tu não disse. Explica melhor?", "certo. O que tu quer fazer a respeito?", "ahh, e isso aconteceu hoje ou já faz tempo?", "qual foi tua reação na hora?", "o que mais pesou nessa situação?", "quero acompanhar direitinho, pode continuar", "hmm, e como tu vê isso agora?"
];

export function expandedFallback(message, profile = {}) {
  const text = String(message || "").toLocaleLowerCase("pt-BR").normalize("NFD").replace(/[\u0300-\u036f]/g, "");
  const recent = Array.isArray(profile.recentReplies) ? profile.recentReplies : [];
  const choose = (options) => {
    const fresh = options.filter(option => !recent.includes(option));
    const pool = fresh.length ? fresh : options;
    const selected = pool[Math.floor(Math.random() * pool.length)];
    profile.recentReplies = [...recent, selected].slice(-40);
    return selected;
  };
  const match = BANKS.find(bank => bank.test.test(text));
  return choose(match ? match.replies : GENERIC);
}
