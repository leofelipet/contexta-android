# Roadmap Android

O web é o painel admin. O app copia o uso no celular, em fatias. Uma fatia = um APK pra testar. Update continua pelos GitHub Releases.

No celular o web já usa 4 abas: Conversas, Contatos, Tarefas, Busca. O resto fica em **Mais**.

Não clonar 1:1: passkeys, gráficos, cliente de e-mail, enviar WhatsApp (não tem REST), FCM (Pushover já cobre o telefone).

| # | Fatia | Pronto quando |
|---|---|---|
| 1 | Casca: 4 abas + Mais | Troca aba, conversas e SSE iguais, Sair funciona |
| 2 | Lista de conversas = web | Filtro direta/grupo, empresa, bloqueado |
| 3 | Thread = web | Transcrição, empresa, contato, bloquear |
| 4 | Contatos | Lista, detalhe, ir pra conversa |
| 5 | Busca de mensagens | Texto + direção/tipo, abre o thread |
| 6 | Tarefas do dia | Lista abertas, concluir |
| 7 | Tarefas criar/editar | Nasce no celular, confere no web |
| 8 | Recorrentes | Lista, ligar/desligar, apagar |
| 9 | Lembretes | Criar e receber no Pushover |
| 10 | Empresas | Lista, detalhe, vínculos |
| 11 | Memórias | Lista, nota, busca |
| 12 | Visão geral | Cards (sem gráfico) |
| 13 | Manutenção | Deny list e limpeza |
| 14 | Integrações | Status WhatsApp/MCP/e-mail/Pushover + teste |
| 15 | Sistema | Saúde e atividade. Acesso = “use o web” |
| 17 | Só se doer | SSE incremental, mídia, enviar WhatsApp, FCM |

Ordem: 1–6 = web mobile. 7–12 = organização. 13–15 = painel. 17 opcional. Update pelo Go saiu.

Antes da 2: voltar do sistema, cache em memória, skeleton, scroll infinito, separador de dia, fundo do chat, ícones.
