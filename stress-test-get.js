import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 50, 
  duration: '10s', 
};

export default function () {
  const token = __ENV.TOKEN;

  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  const response = http.get('http://tarefas:8080/tarefas', params); 
  
  check(response, { 'status was 200': (r) => r.status == 200 });
  sleep(0.1);
}

  // docker run --rm -i --network gftjava_lista-de-tarefas-network -e TOKEN="" grafana/k6 run - <stress-test-get.js