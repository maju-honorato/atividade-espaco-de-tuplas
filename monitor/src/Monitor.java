import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.discovery.LookupLocator;
import net.jini.space.JavaSpace;
import java.util.HashSet;
import java.util.Set;

/**
 * Observa o espaço de tuplas a cada 3 segundos e exibe quantas tarefas pendentes existem.
 *
 * O monitor NÃO deve remover tarefas do espaço — apenas observar.
 *
 * Nível 2B: localize o comentário TODO abaixo e complete a implementação.
 */
public class Monitor {

    private static final String REGGIE_HOST = "reggie";
    private static final int INTERVALO_SEGUNDOS = 3;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.policy", "security.policy");
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        JavaSpace space = descobrirEspaco();
        System.out.println("[MONITOR] Espaço encontrado. Iniciando monitoramento a cada "
            + INTERVALO_SEGUNDOS + "s...");

        while (true) {
            Thread.sleep(INTERVALO_SEGUNDOS * 1000);
            int pendentes = contarTarefas(space);
            System.out.println("[MONITOR] Tarefas pendentes no espaço: " + pendentes);
        }
    }

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

    private static JavaSpace descobrirEspaco() throws Exception {
        LookupLocator locator = new LookupLocator("jini://" + REGGIE_HOST);
        ServiceTemplate template = new ServiceTemplate(null, new Class[]{JavaSpace.class}, null);

        for (int tentativa = 1; tentativa <= 20; tentativa++) {
            try {
                ServiceRegistrar registrar = locator.getRegistrar();
                Object service = registrar.lookup(template);
                if (service != null) {
                    return (JavaSpace) service;
                }
            } catch (Exception e) {
                // reggie ou javaspaces ainda não prontos
            }
            System.out.println("[MONITOR] Aguardando espaço... (" + tentativa + "/20)");
            Thread.sleep(2000);
        }
        throw new RuntimeException("[MONITOR] Não foi possível encontrar o espaço de tuplas.");
    }
}
