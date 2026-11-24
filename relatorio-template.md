# Relatório — Atividade Prática: Espaço de Tuplas com Apache River

**Disciplina:**
**Dupla:** /
**Data:**

---

## Nível 0 — Rodar e observar

### Observação inicial

1. Qual serviço aparece nos logs primeiro? Por que ele precisa existir antes dos outros?

> _Resposta:_ o reggie aparece primeiro, pq o java precisa dele pra se registrar na rede, e o produtor e consumidor depencdem para achar o espaço de tuplas

2. O produtor menciona o nome ou o endereço do consumidor em algum momento? O consumidor menciona o produtor?

> _Resposta:_ nao, nenhum dos dois; eles tao interagindo so com o espaço de tuplas, faz sentido ate pensando na topologia de rede que está na pagina do git

3. O consumidor começa a processar tarefas antes que o produtor termine de depositar todas? O que isso diz sobre como os dois se coordenam?

> _Resposta:_ sim, o consumidor tira a tarefa 1 e começa a processas enquanto o produtor esta mandando outras tarefas; eles trabalham de forma assincrona e concorrente

---

### Experimento de desacoplamento temporal

4. O consumidor encontrou as tarefas mesmo sendo iniciado depois que o produtor já havia encerrado? O que isso demonstra?

> _Resposta:_ sim, pq as tarefas continuam no espaço de tuplas - demonstra o desacoplamento temporal

5. Em comunicação direta via socket, seria possível esse comportamento? Por quê não?

> _Resposta:_ nao, pq por sockets ele iria exigir que os dois estivessem ativos ao mesmo tempo para transmissao

---

## Nível 1 — Inspecionar

### 1.1 As três operações

Preencha a tabela com base no que você observou nos logs:

| Operação River | Equivalente Linda | O que ela faz? | Bloqueia quando não encontra correspondência? | Altera o estado do espaço? |
|---------------|-------------------|----------------|----------------------------------------------|---------------------------|
| `write(entry)` | `OUT` | coloca tupla nova | nao | sim |
| `take(template)` | `IN` | busca tupla compativel, remove do espaço e retorna | sim | sim |
| `read(template)` | `RD` | busca tupla compativel e retorna uma cópia | sim | nao |

---

### 1.2 O papel do `reggie`

1. Quando o `reggie` caiu, os serviços que já estavam conectados ao espaço continuaram funcionando? Por quê?

> _Resposta:_ sim, pq ele participa na descoberta, quando os clientes ja tem as referencias, se comunicam diretamente com o espaço de tupla

2. O que aconteceria com um produtor ou consumidor que tentasse iniciar enquanto o `reggie` estivesse fora do ar?

> _Resposta:_ nao conseguiria localizar o espaço de tuplas e ficaria em loop

3. Qual sistema moderno cumpre papel equivalente ao `reggie` em uma arquitetura de microsserviços?

> _Resposta:_ ferramentes de service descovery com registro dinamico, que dai os serviços registram os endereços e clientes consultamp elo nome sem precisar de ips no codigo

---

### 1.3 Desacoplamento espacial

1. O produtor tem qualquer informação sobre quantos consumidores existem?

> _Resposta:_ nao, so ve o espaço de tuplas e coloca as tarefas la

2. O consumidor tem qualquer informação sobre quem produziu a tarefa que ele retirou?

> _Resposta:_ nao, a tupla so tem os dados da propria tarefa

3. Como produtor e consumidor se coordenam se não se conhecem?

> _Resposta:_ se coordenam pelo espaço de tuplas e pelo padrao das tuplas

---

### 1.4 Comportamento de bloqueio

1. O que o consumidor fez enquanto o espaço estava vazio?

> _Observado:_ ele ficou parado, nao tava gerando erros masficou num loop

2. Quando o produtor depositou a primeira tarefa, o que aconteceu imediatamente?

> _Observado:_ o consumidor saiu do loop e retirou a primeira tarefa

3. Esse comportamento tem nome no modelo Linda. Qual é e por que ele é útil em sistemas distribuídos reais?

> _Resposta:_ para nao precisar de busywaiting continuamente pergutando se tem algo novo, oq acaba economizando processamente e trafego

---

### 1.5 Escalabilidade horizontal

1. Uma mesma tarefa foi processada por dois consumidores ao mesmo tempo?

> _Observado:_ nao, pq o take remove a tupla do espaço, entao quando um pega o outro nao vai conseguir pegar

2. O produtor precisou ser modificado para suportar dois consumidores?

> _Resposta:_ nao, o produtor continua depositando as tarefas no espaço compartilhado

3. Esse comportamento tem um nome em arquitetura de sistemas. Qual é?

> _Resposta:_ competing consumer - quantos mais consumidores, mais tarefas sao processadas sem alterar o produtor

---

## Nível 2 — Modificar

### 2.1 Modificação A — Prioridade de tarefas

1. As tarefas de prioridade alta foram processadas antes das de prioridade baixa? Cole um trecho dos logs que evidencie isso:

sim, sendo 1 a prioridade mais alta, o consumidor pegou a tarefa 4 antes da 3 por ter mais prioridade

```
produtor-1    | [PRODUTOR] write: TaskEntry{id=3, tipo="calcular", prioridade=2}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 2
produtor-1    | [PRODUTOR] write: TaskEntry{id=4, tipo="calcular", prioridade=1}
consumidor-1  | [CONSUMIDOR-1] take: TaskEntry{id=4, tipo="calcular", prioridade=1}
produtor-1    | [PRODUTOR] write: TaskEntry{id=5, tipo="calcular", prioridade=1}
consumidor-1  | [CONSUMIDOR-1] Processamento concluído: tarefa 4
```

2. O produtor precisou ser modificado para que isso funcionasse?

> _Resposta:_ nao, as tuplas ja tinham o campo prioridade definido, mas era somente ignorado

3. Como o consumidor consegue selecionar apenas tarefas de uma prioridade específica? Qual mecanismo do espaço de tuplas torna isso possível?

> _Resposta:_ o consumidor faz um template com o campo prioridade preenchido com 1 e o resto como null

---

### 2.2 Modificação B — Serviço monitor

1. Qual operação você usou no monitor — `read()` ou `take()`? Por quê a outra seria problemática?

> _Resposta:_ foi usado read, pq o take iria remover a tupla do espaço, ent o monitor estaria pegadno tarefas pendentes que nao deveria

2. Como você contou as tarefas pendentes usando apenas `read()`? Que limitação isso revela?

> _Resposta:_ chamando num loop com um hashset pra pegar ids unicos lidos; nao vai ter garantia de ordem pq o servidor pode devolver sempre a mesma, entao cada uma tem q ter id unico e o cliente tem que controlas duplicatas

3. Por que um espaço de tuplas puro não tem operação `count()`? O que seria necessário adicionar ao modelo para suportá-la?

> _Resposta:_ pq foi projetado com operações atomicas e locais para concorrecnia e desacoplamento, e o count exigira observar o estado global de todo espaço num momento especifico; talvez adicionars snapshots consistentes

Cole o trecho do `Monitor.java` que você completou:

```java
private static int contarTarefas(JavaSpace space) throws Exception {
        TaskEntry template = new TaskEntry(null, null, null);
        int count = 0;

        Set<Integer> idsVistos = new HashSet<>();
        int tentativas = 0;

        while (tentativas < 20) {
            TaskEntry encontrada = (TaskEntry) space.read(template, null, 0);
            if (encontrada == null) {
                break; 
            }
            if (idsVistos.add(encontrada.id)) {
                count++;
                tentativas = 0;
            } else {
                tentativas++;
            }
        }

        return count;
    }
```

---

## Observações livres

_(Comportamentos inesperados, erros encontrados, dificuldades técnicas — descreva o que aconteceu e como você resolveu)_

> fazendo o monitor inicialmente usei uma leitura simples pra controlar os ids, mas como nao removia ele estava contando a msm tarefa varias vezes; a soluçao era simples, foi uma desatençao mesmo; ia fazer com uma lista, que nesse exemplo ate funciona pq é pequeno, mas como uma hash é mais eficiente achei interessante colocar assim (apesar que ora esses testes nao faz mta diferença) 

---

## Dúvida para a próxima aula

_(Formule uma pergunta substantiva que surgiu durante a atividade)_

> ja expliquei no anterior que estou fazedno pra estudar pra prova, então como nao vai ter proxima aula, nao sei se faz sentido responder
