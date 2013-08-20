package name.kazennikov.fsa;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Objects;

public class IndexedFSA {
	public static class State {
		int number;
		List<Transition> transitions = new ArrayList<Transition>();
		TIntSet finals;

		public boolean isFinal() {
			return !finals.isEmpty();
		}

		public Transition addTransition(State to, int label) {
			Transition t = new Transition(this, label, to);
			transitions.add(t);
			return t;
		}


		public void toDot(PrintWriter pw, Set<State> visited) {
			if(visited.contains(this))
				return;

			visited.add(this);

			for(Transition t : transitions) {
				pw.printf("%d -> %d [label=\"%d\"];%n", number, t.dest.number, t.label);
			}

			for(Transition t : transitions) {
				t.dest.toDot(pw, visited);
			}



			if(isFinal()) {
				pw.printf("%d [shape=doublecircle];%n", number);
			}
		}
		
		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.add("number", number)
					.add("isFinal", isFinal())
					.toString();
		}
		
		public List<Transition> getTransitions() {
			return transitions;
		}
		
		public TIntSet getFinals() {
			return finals;
		}
		
		public int getNumber() {
			return number;
		}
				
		
		public void setFinalFrom(Set<State> currentDState) {
			for(State c : currentDState) {
				if(c.isFinal()) {
					this.finals.addAll(c.finals);
				}
			}
		}
	}
	
	public static class Transition {
		State src;
		int label;
		State dest;
		
		public Transition(State src, int label, State dest) {
			this.src = src;
			this.label = label;
			this.dest = dest;
		}
		
		
		public State getSrc() {
			return src;
		}
		
		public State getDest() {
			return dest;
		}
		
		public int getLabel() {
			return label;
		}
		
		public boolean isEpsilon() {
			return label == Constants.EPSILON;
		}
		
		@Override
		public String toString() {
			return String.format("{src=%d, label=%d, dest=%d}", src.getNumber(), label, dest.getNumber());
		}
		

	}


	List<State> states = new ArrayList<State>();	
	List<Transition> transitions = new ArrayList<Transition>();
	
	TIntArrayList tFrom = new TIntArrayList();
	TIntArrayList tTo = new TIntArrayList();
	TIntArrayList tLabel = new TIntArrayList();
	
	
	
	State start;
	
	public IndexedFSA() {
		this.start = addState();
	}
	
	
	public State addState() {
		State state = new State();
		state.number = states.size();
		states.add(state);
		return state;
	}
	
	
	public void addTransition(State from, State to, int label) {
		Transition t = from.addTransition(to, label);
		transitions.add(t);
		
		tFrom.add(from.number);
		tTo.add(to.number);
		tLabel.add(label);
	}
	


	public void toDot(PrintWriter pw) {
		pw.println("digraph finite_state_machine {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle]");
		
		for(State s : states) {
			for(Transition t : s.transitions) {
				pw.printf("%d -> %d [label=\"%d\"];%n", t.src.number, t.dest.number, t.label);
			}

			if(s.isFinal()) {
				pw.printf("%d [shape=doublecircle];%n", s.number);
			}

		}
		pw.println("}");

	}
	
	public void toDot(String fileName) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(fileName);
		toDot(pw);
		pw.close();
	}
	
	public Set<State> lambdaClosure(Set<State> states) {
		LinkedList<State> list = new LinkedList<State>(states);
		Set<State> closure = new HashSet<State>(states);

		while(!list.isEmpty()) {
			State current = list.removeFirst();
			for(Transition t : current.transitions) {
				if(t.isEpsilon()) {
					State target = t.dest;
					if(!closure.contains(target)) {
						closure.add(target);
						list.addFirst(target);
					}
				}
			}
		}
		return closure;
	}
	
	private TIntSet labels(Set<State> states) {
		TIntHashSet set = new TIntHashSet();
		
		for(State s : states) {
			for(Transition t : s.transitions) {
				if(!t.isEpsilon())
					set.add(t.label);
			}
		}
		
		return set;
	}
	
	private Set<State> next(Set<State> states, int label) {
		Set<State> next = new HashSet<State>();
		
		for(State s : states) {
			for(Transition t : s.transitions) {
				if(t.label == label)
					next.add(t.dest);
			}
		}
		
		return next;
	}
	
	
	public void determinize(IndexedFSA fsm) {
		
		
		Map<Set<State>, State> newStates = new HashMap<Set<State>, State>();
		Set<Set<State>> dStates = new HashSet<Set<State>>();
		LinkedList<Set<State>> unmarkedDStates = new LinkedList<Set<State>>();
		
		Set<State> currentDState = new HashSet<State>();


		currentDState.add(start);
		currentDState = lambdaClosure(currentDState);
		dStates.add(currentDState);
		unmarkedDStates.add(currentDState);
		
		newStates.put(currentDState, fsm.start);
		fsm.start.setFinalFrom(currentDState);
		
		while(!unmarkedDStates.isEmpty()) {
			currentDState = unmarkedDStates.removeFirst();
			TIntSet labels = labels(currentDState);

			for(int label : labels.toArray()) {
				Set<State> next = next(currentDState, label);
				next = lambdaClosure(next);

				// add new state to epsilon-free automaton
				if(!dStates.contains(next)) {
					dStates.add(next);
					unmarkedDStates.add(next);
					State newState = fsm.addState();
					newStates.put(next, newState);
					newState.setFinalFrom(next);
				}

				State currentState = newStates.get(currentDState);
				State newState = newStates.get(next);
				fsm.addTransition(currentState, newState, label);
			}
		}
	}

	
	/**
	 * Converts this epsilon-NFA to epsilon-free NFA. Non-destructive procedure
	 * 
	 * @return fresh epsilon free NFA
	 */
	public void epsilonFreeFSM(IndexedFSA fsm) {

		Map<Set<State>, State> newStates = new HashMap<Set<State>, State>();
		Set<Set<State>> dStates = new HashSet<Set<State>>();
		LinkedList<Set<State>> unmarkedDStates = new LinkedList<Set<State>>();
		Set<State> currentDState = new HashSet<State>();


		currentDState.add(start);
		currentDState = lambdaClosure(currentDState);
		dStates.add(currentDState);
		unmarkedDStates.add(currentDState);

		newStates.put(currentDState, fsm.start);

		fsm.start.setFinalFrom(currentDState);

		while(!unmarkedDStates.isEmpty()) {
			currentDState = unmarkedDStates.removeFirst();

			for(State state: currentDState) {
				for(Transition t : state.transitions) {

					// skip epsilon transitions
					if(t.isEpsilon())
						continue;


					State target = t.dest;
					Set<State> newDState = new HashSet<State>();
					newDState.add(target);
					newDState = lambdaClosure(newDState);

					// add new state to epsilon-free automaton
					if(!dStates.contains(newDState)) {
						dStates.add(newDState);
						unmarkedDStates.add(newDState);
						State newState = fsm.addState();
						newStates.put(newDState, newState);
						newState.setFinalFrom(newDState);
					}

					State currentState = newStates.get(currentDState);
					State newState = newStates.get(newDState);
					fsm.addTransition(currentState, newState, t.label);
				}
			}

		}
	}
	
	public State getStart() {
		return start;
	}
	
	public int size() {
		return states.size();
	}
	
	public void rev(IndexedFSA fsm) {
		
		List<State> finals = new ArrayList<State>();

		for(int i = 0; i < states.size(); i++ ) {
			State s = fsm.addState();
			State s0 = states.get(i);

			if(s0.isFinal()) {
				s.finals = s0.finals;
				finals.add(s);
			}
		}
		
		
		for(int i = 0; i < tFrom.size(); i++) {
			State from = fsm.states.get(tTo.get(i) + 1);
			int label = tLabel.get(i);
			State to = fsm.states.get(tFrom.get(i) + 1);
			fsm.addTransition(from, to, label);
		}
		
		for(State f : finals) {
			fsm.addTransition(fsm.start, f, Constants.EPSILON);
		}
		
	}
	
	public List<State> finals() {
		List<State> finals = new ArrayList<State>();
		
		for(State s : states) {
			if(s.isFinal())
				finals.add(s);
		}
		
		return finals;
	}
	
	
	
	
	private void trReverse() {
		for(State s : states) {
			s.transitions.clear();
		}
	
		
		for(Transition t : transitions) {
			State temp = t.dest;
			t.dest = t.src;
			t.src = temp;
			
			states.get(t.src.number).transitions.add(t);
		}
		
		
	
	}
	
	private void trSort() {
		Collections.sort(transitions, new Comparator<Transition>() {

			@Override
			public int compare(Transition o1, Transition o2) {
				int res = Integer.compare(o1.src.number, o2.src.number);
				if(res != 0)
					return res;
				res = Integer.compare(o1.label, o2.label);
				if(res != 0)
					return res;
				res = Integer.compare(o1.dest.number, o2.dest.number);
				
				return res;
			}
		});
	}
	
	public class AutomatonMinimizationData {
		
		TIntIntHashMap labelsMap = new TIntIntHashMap();
		// states:
		protected int[] statesClassNumber; // state -> class
		
		/*
		 *  linked list for state classes
		 *  stateNext[i] - next member of class for i-th state
		 *  statePrev[i] - previous member of class for i-th state
		 */
		protected int[] statesNext; // следующее состояния данного класса
		protected int[] statesPrev; // предыдущее состояние этого класса
		
		protected int statesStored; // число классов

		// classes:
		protected int[] classesFirstState; // номер первого состояния для класса i
		protected int[] classesPower; // размер класса (число состояний в классе)
		protected int[] classesNewPower;
		protected int[] classesNewClass;
		protected int[] classesFirstLabel;
		protected int[] classesNext; // class index sequence i.e. classesNext[cls] - next class number for given class
		protected int classesStored; // число классов
		protected int classesAlloced;
		protected int firstClass; // first class index

		// letters:
		protected int[] labelsLabel; // label index -> label id. label index is a sequence number for in [class x labels]
		protected int[] labelsNext; // labels next label[labelId] -> next label id for this class
		protected int labelsStored; // total number of label ids, initially [class x labels]
		protected int labelsAlloced;

		public AutomatonMinimizationData(int statesStored) {
			this.statesStored = statesStored;
			statesClassNumber = new int[statesStored];
			statesNext = new int[statesStored];
			statesPrev = new int[statesStored];

			classesAlloced = 1024;
			classesFirstState = new int[classesAlloced];
			classesPower = new int[classesAlloced];
			classesNewPower = new int[classesAlloced];
			classesNewClass = new int[classesAlloced];
			classesFirstLabel = new int[classesAlloced];
			classesNext = new int[classesAlloced];
			firstClass = Constants.NO;

			labelsAlloced = 1024;
			labelsLabel = new int[labelsAlloced];
			labelsNext = new int[labelsAlloced];
		}
		
		/*
		 * Процедуры строят цепочки состояний и переходов в обратном порядке.
		 * Т.е. firstClass - на самом деле последнее по порядку добавления.
		 * Таким образом получается, что идут цепочки:
		 * 1. классов. от firstClass по classsesNext
		 * 2. classesFirstState - первое состояние класса. Можно обходить по classes[
		 */
		/**
		 * Adds state to given state class
		 * 
		 * @param state state number
		 * @param cls class number
		 */
		protected void addState(int state, int cls) {
			
			// linked list addFirst() method
			statesNext[state] = classesFirstState[cls];
			if (classesFirstState[cls] != Constants.NO) {
				statesPrev[classesFirstState[cls]] = state;
			}
			statesPrev[state] = Constants.NO;
			
			statesClassNumber[state] = cls;
			classesFirstState[cls] = state;
			classesPower[cls]++;
		}

		protected void addLabel(int cls, int label) {
			// reallocate letters if needed
			if (labelsStored == labelsAlloced) {
				int mem = labelsAlloced + labelsAlloced / 4; // 1.25 growth rate
				labelsLabel = GenericWholeArrray.realloc(labelsLabel, mem, labelsStored);
				labelsNext = GenericWholeArrray.realloc(labelsNext, mem, labelsStored);
				labelsAlloced = mem;
			}
			
			// установить первую метку перехода для класса (первое появление класса)
			if (classesFirstLabel[cls] == Constants.NO) {
				classesNext[cls] = firstClass;
				firstClass = cls;
			}
			
			labelsLabel[labelsStored] = label;
			
			labelsNext[labelsStored] = classesFirstLabel[cls];
			classesFirstLabel[cls] = labelsStored;
			
			labelsStored++;
		}

		protected void reallocClasses() {
			int mem = classesAlloced + classesAlloced / 4;
			
			classesFirstState = GenericWholeArrray.realloc(classesFirstState, mem, classesStored);
			classesPower = GenericWholeArrray.realloc(classesPower, mem, classesStored);
			classesNewPower = GenericWholeArrray.realloc(classesNewPower, mem, classesStored);
			classesNewClass = GenericWholeArrray.realloc(classesNewClass, mem, classesStored);
			classesFirstLabel = GenericWholeArrray.realloc(classesFirstLabel, mem, classesStored);
			classesNext = GenericWholeArrray.realloc(classesNext, mem, classesStored);
			classesAlloced = mem;
		}

		protected void moveState(int state, int newClass) {
			int curClass = statesClassNumber[state];
			
			if (statesPrev[state] == Constants.NO) {
				classesFirstState[curClass] = statesNext[state];
			} else {
				statesNext[statesPrev[state]] = statesNext[state];
			}
			
			if (statesNext[state] != Constants.NO) {
				statesPrev[statesNext[state]] = statesPrev[state];
			}
			
			addState(state, newClass);
		}
		
		public void mapLabels() {
			labelsMap.put(0, 0);
			
			// map labels to [0 ... n] values for correct algorithm work
			for(Transition t : transitions) {
				
				if(!labelsMap.containsKey(t.label)) {
					int label = labelsMap.size();
					labelsMap.put(t.label, label);
					t.label = label;
				} else {
					t.label = labelsMap.get(t.label);
				}
			}
		}
		
		public void unmapLabels() {
			final TIntIntHashMap map = new TIntIntHashMap();
			labelsMap.forEachEntry(new TIntIntProcedure() {
				
				@Override
				public boolean execute(int a, int b) {
					map.put(b, a);
					return true;
				}
			});
			
			for(Transition t : transitions) {
				t.label = map.get(t.label);
			}
		}

		public void initClass(int cls) {
			classesFirstState[cls] = Constants.NO;
			classesNewClass[cls] = Constants.NO;
			classesNewPower[cls] = 0;
			classesPower[cls] = 0;
			classesFirstLabel[cls] = Constants.NO;
			classesNext[cls] = Constants.NO;
		}
		
		public int[] finalties() {
			int[] finalties = new int[states.size()];
			for(int state = 0; state < states.size(); state++) {
				State s = states.get(state);
				finalties[state] = -1;
				if (s.isFinal()) {
					finalties[state] = s.number;
				}
			}
			
			return finalties;
		}
		
		public IntSequence countClasses(int[] finalties) {
			IntSequence classes = new IntSequence(); // существующие классы. изначально - final - каждый в отдельный класс
			
			for(int state = 0; state < states.size(); state++) {
				classes.addIfDoesNotExsist(finalties[state]);
			}
			
			return classes;

		}
		
		
	}

	
	protected AutomatonMinimizationData hopcroftMinimize() {
		// reverse transitions
		trReverse();
		trSort();
		
		AutomatonMinimizationData data = new AutomatonMinimizationData(states.size());
		data.mapLabels();
		
		int labelsStored = data.labelsMap.size();
		int[] finalties = data.finalties();
		IntSequence classes = data.countClasses(finalties);
		
		if(classes.seqStored == 1) {
			return data;
		}

		// инициализаця данных о минимизации
		for(int cls = 0; cls < classes.seqStored; cls++) {
			data.initClass(cls);
		}
		
		data.classesStored = classes.seqStored;

		// добавить состояния по классам
		for(int state = 0; state < states.size(); state++) {
			data.addState(state, classes.indexOf(finalties[state]));
		}

		// добавить метки переходов (по классам)?
		for(int label = 1; label < labelsStored; label++) {
			for(int cls = 0; cls < data.classesStored; cls++) {
				data.addLabel(cls, label);
			}
		}
		
		IntSequence states = new IntSequence();
		classes.seqStored = 0;

		GenericWholeArrray alpha = new GenericWholeArrray(GenericWholeArrray.TYPE_BIT, labelsStored);
		
		while(data.firstClass != Constants.NO) {
			int q1 = data.firstClass; 
			int a = data.labelsLabel[data.classesFirstLabel[q1]];
			data.classesFirstLabel[q1] = data.labelsNext[data.classesFirstLabel[q1]];
			
			if(data.classesFirstLabel[q1] == Constants.NO) {
				data.firstClass = data.classesNext[q1];
			}
			
			classes.seqStored = 0;
			states.seqStored = 0;

			// iterate through states of the class q1
			/*
			 * Группируем переходы в q1 по a. по различным классам
			 */
			for(int state = data.classesFirstState[q1]; state != Constants.NO; state = data.statesNext[state]) {
				State s = this.states.get(state);

				for(Transition t : s.transitions) {
					if(t.label == a) {
						int q0 = data.statesClassNumber[t.dest.getNumber()];
						states.add(t.dest.getNumber());
						if(data.classesNewPower[q0] == 0) {
							classes.add(q0);
						}
						
						data.classesNewPower[q0]++;
					}
				}
			}
			
			for(int state = 0; state < states.seqStored; state++) {
				int q0 = data.statesClassNumber[states.seq[state]];
				
				if(data.classesNewPower[q0] == data.classesPower[q0]) {
					continue;
				}
				

				if(data.classesNewClass[q0] == Constants.NO) {
					
					if (data.classesStored == data.classesAlloced) {
						data.reallocClasses();
					}
					
					data.classesNewClass[q0] = data.classesStored;					
					data.initClass(data.classesStored);
					
					data.classesStored++;
				}
				
				data.moveState(states.seq[state], data.classesNewClass[q0]);
			}
			
			for(int cls = 0; cls < classes.seqStored; cls++) {
				int q0 = classes.seq[cls];
				
				if(data.classesNewPower[q0] != data.classesPower[q0]) {
					data.classesPower[q0] -= data.classesNewPower[q0];
					
					alpha.clear();
					
					for(int label = data.classesFirstLabel[q0]; label != Constants.NO; label = data.labelsNext[label]) {
						data.addLabel(data.classesNewClass[q0], data.labelsLabel[label]);
						alpha.setElement(data.labelsLabel[label], Constants.NO);
					}
					
					for(int label = 1; label < labelsStored; label++) {
						if(alpha.elementAt(label) == Constants.NO) {
							continue;
						}
						
						if(data.classesPower[q0] < data.classesPower[data.classesNewClass[q0]]) {
							data.addLabel(q0, label);
						} else {
							data.addLabel(data.classesNewClass[q0], label);
						}
						
					}
				}

				data.classesNewPower[q0] = 0;
				data.classesNewClass[q0] = Constants.NO;
			}
		}
		return data;
	}
	
	public void minimize(IndexedFSA fsm) {
		AutomatonMinimizationData data = hopcroftMinimize();
		
		if (data.classesStored == 0) {
			return;
		}
		
		data.unmapLabels();
		trReverse();
		trSort();
		// add states
		for(int i = 0; i < data.classesStored; i++) {
			fsm.addState();
		}

		// add transitions
		for (int i = 0; i < data.classesStored; i++) {
			int state = data.classesFirstState[i];
			State s = states.get(state);

			// set start state
			if(s.number == 0) {
				fsm.start = fsm.states.get(i);
			}
			
			for(Transition t : s.transitions) {
				fsm.addTransition(fsm.states.get(i), fsm.states.get(data.statesClassNumber[t.dest.number]), t.label);
			}
		}
		
		// add finals
		for(int i = 0; i < data.classesStored; i++) {
			int state = data.classesFirstState[i];
			State s0 = states.get(state);

			State s = fsm.states.get(i);
			
			if(s0.isFinal()) {
				s.finals.addAll(s0.finals);
			}
		}

				
	}


}
