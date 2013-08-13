package name.kazennikov.fsm;

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

public class IndexedFSMGeneric<E> {
	
	public static class State<E1> {
		int number;
		List<Transition<E1>> transitions = new ArrayList<Transition<E1>>();
		Set<E1> finals = new HashSet<>();

		public boolean isFinal() {
			return !finals.isEmpty();
		}

		public Transition<E1> addTransition(State<E1> to, int label) {
			Transition<E1> t = new Transition<E1>(this, label, to);
			transitions.add(t);
			return t;
		}
		
		/**
		 * Get next state from this one on given input (threat the state transitions deterministically)
		 * This means that only the first matched input is returned
		 * 
		 * @param input input symbol
		 * 
		 * @return next state, or null if there is no such transition
		 */
		public State<E1> next(int input) {
			for(int i = 0; i < transitions.size(); i++) {
				Transition<E1> t = transitions.get(i);
				
				if(t.label == input)
					return t.dest;
			}
			
			return null;
		}


		public void toDot(PrintWriter pw, Set<State<E1>> visited) {
			if(visited.contains(this))
				return;

			visited.add(this);

			for(Transition<E1> t : transitions) {
				pw.printf("%d -> %d [label=\"%d\"];%n", number, t.dest.number, t.label);
			}

			for(Transition<E1> t : transitions) {
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
		
		public List<Transition<E1>> getTransitions() {
			return transitions;
		}
		
		public Set<E1> getFinals() {
			return finals;
		}
		
		public int getNumber() {
			return number;
		}
				
		
		public void setFinalFrom(Set<State<E1>> currentDState) {
			for(State<E1> c : currentDState) {
				if(c.isFinal()) {
					this.finals.addAll(c.finals);
				}
			}
		}
	}
	
	public static class Transition<E1> {
		/**
		 * Encoding:
		 * <ul>
		 * <li> label > 0 - AnnotationMatcher table lookup
		 * <li> label = 0 - epsilon
		 * <li> label = -1 - GROUP_START
		 * <li> label < -1 - named group lookup
		 */
		State<E1> src;
		int label;
		State<E1> dest;
		
		public Transition(State<E1> src, int label, State<E1> dest) {
			this.src = src;
			this.label = label;
			this.dest = dest;
		}
		
		
		public State<E1> getSrc() {
			return src;
		}
		
		public State<E1> getDest() {
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


	List<State<E>> states = new ArrayList<State<E>>();	
	List<Transition<E>> transitions = new ArrayList<Transition<E>>();
	// используются сейчас только в rev()
	TIntArrayList tFrom = new TIntArrayList();
	TIntArrayList tTo = new TIntArrayList();
	TIntArrayList tLabel = new TIntArrayList();
	
	
	
	State<E> start;
	
	public IndexedFSMGeneric() {
		this.start = addState();
	}
	
	
	public State<E> addState() {
		State<E> state = new State<E>();
		state.number = states.size();
		states.add(state);
		return state;
	}
	
	
	public void addTransition(State<E> from, State<E> to, int label) {
		Transition<E> t = from.addTransition(to, label);
		transitions.add(t);
		
		tFrom.add(from.number);
		tTo.add(to.number);
		tLabel.add(label);
	}
	


	public void toDot(PrintWriter pw) {
		pw.println("digraph finite_state_machine {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle]");
		
		for(State<E> s : states) {
			for(Transition<E> t : s.transitions) {
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
	
	public Set<State<E>> lambdaClosure(Set<State<E>> states) {
		LinkedList<State<E>> list = new LinkedList<State<E>>(states);
		Set<State<E>> closure = new HashSet<State<E>>(states);

		while(!list.isEmpty()) {
			State<E> current = list.removeFirst();
			for(Transition<E> t : current.transitions) {
				if(t.isEpsilon()) {
					State<E> target = t.dest;
					if(!closure.contains(target)) {
						closure.add(target);
						list.addFirst(target);
					}
				}
			}
		}
		return closure;
	}
	
	private TIntSet labels(Set<State<E>> states) {
		TIntHashSet set = new TIntHashSet();
		
		for(State<E> s : states) {
			for(Transition<E> t : s.transitions) {
				if(!t.isEpsilon())
					set.add(t.label);
			}
		}
		
		return set;
	}
	
	private Set<State<E>> next(Set<State<E>> states, int label) {
		Set<State<E>> next = new HashSet<State<E>>();
		
		for(State<E> s : states) {
			for(Transition<E> t : s.transitions) {
				if(t.label == label)
					next.add(t.dest);
			}
		}
		
		return next;
	}
	
	
	public void determinize(IndexedFSMGeneric<E> fsm) {
		
		
		Map<Set<State<E>>, State<E>> newStates = new HashMap<Set<State<E>>, State<E>>();
		Set<Set<State<E>>> dStates = new HashSet<Set<State<E>>>();
		LinkedList<Set<State<E>>> unmarkedDStates = new LinkedList<Set<State<E>>>();
		
		Set<State<E>> currentDState = new HashSet<State<E>>();


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
				Set<State<E>> next = next(currentDState, label);
				next = lambdaClosure(next);

				// add new state to epsilon-free automaton
				if(!dStates.contains(next)) {
					dStates.add(next);
					unmarkedDStates.add(next);
					State<E> newState = fsm.addState();
					newStates.put(next, newState);
					newState.setFinalFrom(next);
				}

				State<E> currentState = newStates.get(currentDState);
				State<E> newState = newStates.get(next);
				fsm.addTransition(currentState, newState, label);
			}
		}
	}

	
	/**
	 * Converts this epsilon-NFA to epsilon-free NFA. Non-destructive procedure
	 * 
	 * @return fresh epsilon free NFA
	 */
	public void epsilonFreeFSM(IndexedFSMGeneric<E> fsm) {

		Map<Set<State<E>>, State<E>> newStates = new HashMap<Set<State<E>>, State<E>>();
		Set<Set<State<E>>> dStates = new HashSet<Set<State<E>>>();
		LinkedList<Set<State<E>>> unmarkedDStates = new LinkedList<Set<State<E>>>();
		Set<State<E>> currentDState = new HashSet<State<E>>();


		currentDState.add(start);
		currentDState = lambdaClosure(currentDState);
		dStates.add(currentDState);
		unmarkedDStates.add(currentDState);

		newStates.put(currentDState, fsm.start);

		fsm.start.setFinalFrom(currentDState);

		while(!unmarkedDStates.isEmpty()) {
			currentDState = unmarkedDStates.removeFirst();

			for(State<E> state: currentDState) {
				for(Transition<E> t : state.transitions) {

					// skip epsilon transitions
					if(t.isEpsilon())
						continue;


					State<E> target = t.dest;
					Set<State<E>> newDState = new HashSet<State<E>>();
					newDState.add(target);
					newDState = lambdaClosure(newDState);

					// add new state to epsilon-free automaton
					if(!dStates.contains(newDState)) {
						dStates.add(newDState);
						unmarkedDStates.add(newDState);
						State<E> newState = fsm.addState();
						newStates.put(newDState, newState);
						newState.setFinalFrom(newDState);
					}

					State<E> currentState = newStates.get(currentDState);
					State<E> newState = newStates.get(newDState);
					fsm.addTransition(currentState, newState, t.label);
				}
			}

		}
	}
	
	public State<E> getStart() {
		return start;
	}
	
	public int size() {
		return states.size();
	}
	
	public void rev(IndexedFSMGeneric<E> fsm) {
		
		List<State<E>> finals = new ArrayList<State<E>>();

		for(int i = 0; i < states.size(); i++ ) {
			State<E> s = fsm.addState();
			State<E> s0 = states.get(i);

			if(s0.isFinal()) {
				s.finals = s0.finals;
				finals.add(s);
			}
		}
		
		
		for(int i = 0; i < tFrom.size(); i++) {
			State<E> from = fsm.states.get(tTo.get(i) + 1);
			int label = tLabel.get(i);
			State<E> to = fsm.states.get(tFrom.get(i) + 1);
			fsm.addTransition(from, to, label);
		}
		
		for(State<E> f : finals) {
			fsm.addTransition(fsm.start, f, Constants.EPSILON);
		}
		
	}
	
	public List<State<E>> finals() {
		List<State<E>> finals = new ArrayList<State<E>>();
		
		for(State<E> s : states) {
			if(s.isFinal())
				finals.add(s);
		}
		
		return finals;
	}
	
	
	
	
	private void trReverse() {
		for(State<E> s : states) {
			s.transitions.clear();
		}
	
		
		for(Transition<E> t : transitions) {
			State<E> temp = t.dest;
			t.dest = t.src;
			t.src = temp;
			
			states.get(t.src.number).transitions.add(t);
		}
		
		
	
	}
	
	private void trSort() {
		Collections.sort(transitions, new Comparator<Transition<E>>() {

			@Override
			public int compare(Transition<E> o1, Transition<E> o2) {
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
			for(Transition<E> t : transitions) {
				
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
			
			for(Transition<E> t : transitions) {
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
				State<E> s = states.get(state);
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
				State<E> s = this.states.get(state);

				for(Transition<E> t : s.transitions) {
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
	
	public void minimize(IndexedFSMGeneric<E> fsm) {
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
			State<E> s = states.get(state);

			// set start state
			if(s.number == 0) {
				fsm.start = fsm.states.get(i);
			}
			
			for(Transition<E> t : s.transitions) {
				fsm.addTransition(fsm.states.get(i), fsm.states.get(data.statesClassNumber[t.dest.number]), t.label);
			}
		}
		
		// add finals
		for(int i = 0; i < data.classesStored; i++) {
			int state = data.classesFirstState[i];
			State<E> s0 = states.get(state);

			State<E> s = fsm.states.get(i);
			
			if(s0.isFinal()) {
				s.finals.addAll(s0.finals);
			}
		}

				
	}




}
