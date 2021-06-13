package pbftSimulator;

public class test {
	String name = null;
	
	public test(String name) throws InterruptedException {
		super();
		this.name = name;
		System.out.println(name);
		init();
		Thread.sleep(1880);
		System.out.println(name);
	}
	public void init(){
		new Thread(new Run(this)).start();
	}
	public static void main(String [] args) throws InterruptedException
	{
		new test("main");
	}
	class Run implements Runnable{
		private test r = null;
		
		public Run(test r){
		this.r = r;
		}
		@Override
		public void run() {
		// TODO Auto-generated method stub
		r.name = "run";
		}
		
	};
}