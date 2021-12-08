package appl;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.ArrayList;

import core.Message;

public class SingleUser {

	public static void main(String[] args) {
		new SingleUser();
	}
	
	public SingleUser(){
		// Leitura das informacoes do usuario
		Scanner reader = new Scanner(System.in);  
		System.out.print("Enter the Broker port number: ");
		int brokerPort = reader.nextInt();
		
		System.out.print("Enter the Broker address: ");
		String brokerAdd = reader.next();
		
		System.out.print("Enter the User name: ");
		String userName = reader.next();
		
		System.out.print("Enter the User port number: ");
		int userPort = reader.nextInt();
		
		System.out.print("Enter the User address: ");
		String userAdd = reader.next();
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} // Sleep de 5s
		
		// Cria um PubSubClient com o endereco e a porta informada 
		PubSubClient user = new PubSubClient(userAdd, userPort);
		
		// Me inscrevo no broker
		user.subscribe(brokerAdd, brokerPort);
		
		// Chamo o metodo para iniciar o TP2 passando as informacoes do usuario
		startTP2(user, userName, brokerPort, brokerAdd);
	}
	
	private void startTP2 (PubSubClient user, String userName, int brokerPort, String brokerAdd){
		// Defino as variaveis que o usuario pode estar interessado em utilizar 
		String[] resources = {"var X"};
		
		// Aqui eu escolho as opcoes de variaveis 
		Random seed = new Random();
		
		for(int i =0; i<5; i++){
			// Faz uma publicacao no broker
			String oneResource = resources[seed.nextInt(resources.length)];
			String flag = "acquire";
			Thread sendOneMsg = new ThreadWrapper(user, userName+":"+flag+":"+oneResource, brokerAdd, brokerPort);
			
			// Manda uma mensagem para o broker
			sendOneMsg.start();
			
			// Aguarda o termino do envio 
			try{
				sendOneMsg.join();			
			}catch (Exception e){
				e.printStackTrace();
			}
			
			// Faz a obtencao dos notifies do broker
			List<Message> logUser = user.getLogMessages();
			
			// Aqui vamos fazer o tratamento do Log 
			treatLog(logUser, user, userName, flag, oneResource, brokerAdd, brokerPort, sendOneMsg);
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} // Sleep de 1s
		}
		
		// Faco o unsubscribe e termino o meu PubSubClient				
		user.unsubscribe(brokerAdd, brokerPort);
		
		user.stopPubSubClient();
		
	}
	
	private void treatLog(List<Message> logUser, PubSubClient user, String userName, String flag, String oneResource, String brokerAdd, int brokerPort, Thread sendOneMsg){
		//aqui existe toda a logica do protocolo do TP2
		//se permanece neste metodo ate que o acesso a VAR X ou VAR Y ou VAR Z ocorra
		// Verificar quem ta na fila, quantos aquire tem na lista
		
		List<Message> logAcquire = new ArrayList<Message>(); 
		List<Message> logRelease = new ArrayList<Message>();

		Iterator<Message> it = logUser.iterator();
		while(it.hasNext()){
			Message aux = it.next();
			String content = aux.getContent();

			try {
				String[] part = content.split(":");

				if (part[1].equals("acquire")) {
					logAcquire.add(aux);
				}
				else {
					logRelease.add(aux);
				}
			} catch(Exception e) {}

			//System.out.print(aux.getContent() + aux.getLogId() + " | ");
		}

		while (logAcquire.size() != logRelease.size()){
			Message ultElm = logAcquire.get(logAcquire.size() -1);
			String contentElm = ultElm.getContent();
			String[] nameElm = contentElm.split(":");

			if (nameElm[0].equals(userName)) {
				
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				flag = "release";
				sendOneMsg = new ThreadWrapper(user, userName+":"+flag+":"+oneResource, brokerAdd, brokerPort);

				// Manda uma mensagem para o broker
				sendOneMsg.start();
			
				// Aguarda o termino do envio 
				try{
					sendOneMsg.join();			
				}catch (Exception e){
					e.printStackTrace();
				}

				// Obtem os notifies do broker
				logUser = user.getLogMessages();
				logAcquire.clear();
				logRelease.clear();
				// Colocar o codigo de separar o log user com um 
				Iterator<Message> itAux = logUser.iterator();
				while(itAux.hasNext()){
					Message aux = itAux.next();
					String content = aux.getContent();

					try {
						String[] part = content.split(":");

						if (part[1].equals("acquire")) {
							logAcquire.add(aux);
						}
						else {
							logRelease.add(aux);
						}
					} catch(Exception e) {}

					//System.out.print(aux.getContent() + aux.getLogId() + " | ");
				}

			}
		}

		System.out.println("\nLog acquire: ");
		it = logAcquire.iterator();
		while (it.hasNext()) {
			Message aux = it.next();
			String content = aux.getContent();
			System.out.print(content + aux.getLogId() + " | ");
		}

		System.out.println("\nLog release: ");
		it = logRelease.iterator();
		while (it.hasNext()) {
			Message aux = it.next();
			String content = aux.getContent();
			System.out.print(content + aux.getLogId() + " | ");
		}
	}
	
	class ThreadWrapper extends Thread{
		PubSubClient c;
		String msg;
		String host;
		int port;
		
		public ThreadWrapper(PubSubClient c, String msg, String host, int port){
			this.c = c;
			this.msg = msg;
			this.host = host;
			this.port = port;
		}
		public void run(){
			c.publish(msg, host, port);
		}
	}

}
