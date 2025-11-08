A Jornada do Projeto: De Ferramenta de Estudo a IDE Móvel
Este projeto não começou com a ambição de ser um gerenciador de banco de dados completo. Sua origem foi muito mais simples: eu só queria uma forma de praticar comandos SQL no meu celular.

A Origem: A Frustração Inicial
Como muitos desenvolvedores a aprender SQL, eu queria a flexibilidade de poder testar CREATE TABLE ou SELECT em qualquer lugar, sem estar preso ao PC. A minha ideia inicial foi baixar um "MySQL Manager" qualquer da Play Store, apontá-lo para o servidor MySQL no meu computador (127.0.0.1) e começar a praticar.

O fracasso foi imediato. O 127.0.0.1 não funcionava, as permissões de usuário (root@localhost) bloqueavam-me, e as regras de firewall eram um mistério. Esta frustração inicial foi o catalisador de tudo: "E se, em vez de tentar usar uma ferramenta, eu construísse a minha própria?"

O que começou como um desafio pessoal ("será que consigo fazer esta conexão funcionar?") rapidamente se transformou numa pergunta de portfólio: "Será que consigo construir um app melhor, mais intuitivo e com as funcionalidades que eu, como estudante, gostaria de ter?"

Os Desafios Enfrentados (A Escalada Técnica)
Construir este app foi uma jornada de resolução de problemas, um de cada vez.

O Desafio da Conexão (O Básico): O primeiro desafio foi a rede. Aprender que o localhost de um telemóvel não é o localhost do PC, e mergulhar nas permissões do MySQL (GRANT ALL ON *.*...) e nas regras de Firewall do Windows (Porta 3306) foi o primeiro grande obstáculo.

O "Crash" (O Muro): O desafio mais crítico foi o NetworkOnMainThreadException. O Android não permite operações de rede (como uma conexão JDBC) na thread principal. Isto forçou-me a sair da minha zona de conforto e a mergulhar a fundo nas Coroutines do Kotlin, usando Dispatchers.IO para todas as operações de rede e withContext(Dispatchers.Main) para reportar o sucesso ou o fracasso ao utilizador (com um Toast).

O "Crash" Inesperado (A Compatibilidade): O segundo grande muro foi o java.lang.NoClassDefFoundError: Ljava/sql/SQLType;. O driver JDBC moderno do MySQL (v8.x) é incompatível com o runtime do Android. A depuração disto foi complexa, e a solução foi pesquisar e encontrar uma versão legacy (v5.1.49) que fosse 100% compatível, ensinando-me uma lição valiosa sobre a gestão de dependências em Java/Kotlin.

O Desafio da "Inteligência" (A Joia da Coroa): O "Terminal SQL" foi o maior desafio. Um TextField simples não era suficiente. O meu objetivo era recriar uma experiência de IDE, o que me levou a aprender sobre:

VisualTransformation: Para implementar o syntax highlighting (texto colorido) e a auto-conversão para caixa alta (SELECT).

Controlo de TextFieldValue: O verdadeiro desafio. Para implementar o auto-completar de parênteses () e a auto-indentação (tanto o "sanduíche" \n \t \n como a indentação persistente), tive de criar uma lógica complexa no onValueChange que interceta cada tecla digitada pelo utilizador.

Bugs: Foi isto que levou ao "crash ao colar", pois a minha lógica inteligente não estava preparada para uma mudança de texto em massa. Corrigir isto exigiu adicionar "cláusulas de guarda" para diferenciar a digitação da colagem.

O Que Eu Aprendi (As Recompensas)
Este projeto foi, sem dúvida, o mais denso em aprendizado que já construí. Posso dizer com confiança que aprendi a:

Dominar a Arquitetura de UI Moderna (Jetpack Compose): Eu não aprendi apenas a fazer botões. Aprendi a construir um app complexo de 6 ecrãs com uma Single-Activity Architecture. Aprendi a usar NavHost para passar argumentos (como conexaoId, dbName), a gerir o estado da UI com ViewModel e a criar UIs reativas que se atualizam sozinhas usando Flow e collectAsState.

Gestão de Dados (Local e Remoto): Aprendi a implementar dois fluxos de dados completos.

Local (Room): Modelei e construí um banco de dados local (Room) para o CRUD completo das conexões (@Entity, @Dao, OnConflictStrategy.REPLACE para o Update).

Remoto (JDBC): Aprendi a gerir o ciclo de vida de uma conexão de rede (java.sql.Connection), a executar qualquer tipo de query (statement.execute()), a diferenciar um SELECT (que tem ResultSet) de um UPDATE (que tem updateCount), e a ler os metadados da tabela para desenhar colunas dinamicamente.

A Importância da Experiência do Utilizador (UX): Aprendi que "funcional" não é o mesmo que "bom". O app só se tornou profissional quando:

Filtrei os bancos de sistema (sys, mysql).

Alinhei as tabelas de dados (de "tortas" para width(150.dp)).

Usei Snackbar para feedback positivo e BottomSheet para feedback de erro, mantendo o editor limpo.

A Mentalidade de um Engenheiro de Produto: O app cresceu organicamente. Cada funcionalidade (o filtro, o "Editar", o DROP DATABASE) nasceu de uma necessidade real. Aprendi a ver um projeto não como uma lista de tarefas, mas como um produto em evolução, onde cada nova funcionalidade se constrói sobre a anterior.

O que começou como uma simples ferramenta de treino para mim, evoluiu para um robusto projeto de portfólio que demonstra um domínio completo do desenvolvimento Android moderno, desde a UI até ao core de networking e bases de dados.
