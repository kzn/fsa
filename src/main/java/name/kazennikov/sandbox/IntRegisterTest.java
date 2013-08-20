package name.kazennikov.sandbox;

import name.kazennikov.dafsa.IntRegister;


public class IntRegisterTest {
	public static class TestRegister extends IntRegister {

		@Override
		public int hash(int state) {
			return Math.abs(state);
		}

		@Override
		public boolean equals(int state1, int state2) {
			return Math.abs(state1) == Math.abs(state2);
		}
	}
	
	public static void main(String[] args) {
		TestRegister test = new TestRegister();
		for(int i = 0; i < 1000; i++) {
			test.add(i);
		}
		
		for(int i = 0; i < 1000; i++) {
			if(!test.contains(-i))
				throw new IllegalStateException();
		}
		
		
		
	}

}
