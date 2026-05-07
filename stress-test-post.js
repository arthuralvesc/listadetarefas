import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuração do teste: Simular 50 usuários simultâneos bombardeando a API por 30 segundos
export let options = {
    vus: 5, // Virtual Users
    duration: '10s',
};

export default function () {
    const token = __ENV.TOKEN;
    const url = 'http://18.191.246.79:8080/tarefas'; // Ajuste sua URL e porta
    
    const payload = JSON.stringify({
        nome: `Tarefa de Teste ${__VU} - ${__ITER}`, // Nomes dinâmicos
        prioridade: 'ALTA'
    });

    const params = {
        headers: {
            'Authorization': `Bearer ${token}`, 
            'Content-Type': 'application/json' 
        },
    };

    // Dispara o POST
    let res = http.post(url, payload, params);

    // Verifica se a API sobreviveu e retornou 201 Created
    check(res, {
        'status is 201': (r) => r.status === 201,
    });

    // Pausa um tempo aleatório (entre 0 e 1 segundo) para simular usuários humanos reais
    sleep(Math.random()); 
}

  // docker run --rm -i --network gftjava_lista-de-tarefas-network -e TOKEN="eyJhbGciOiJIUzM4NCJ9.eyJpc3MiOiJsaXN0YS1kZS10YXJlZmFzLWFwaSIsInN1YiI6ImQuc2NocnV0ZUBkdW5kZXJtaWZmbGluLmNvbSIsImlkIjoyLCJpYXQiOjE3NzgxODEzNDQsImV4cCI6MTc3ODE4ODU0NH0.9ntscEFdXOSvfJwKVl_Wt0bFaFEa7bH5ukej6w6HGdqktLZiG_kzfieULa6hkaP2" grafana/k6 run - <stress-test-post.js