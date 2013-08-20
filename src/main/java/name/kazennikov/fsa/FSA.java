package name.kazennikov.fsa;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class FSA<E> {
	
	List<FSAState<E>> states = new ArrayList<FSAState<E>>();	
	List<FSATransition<E>> transitions = new ArrayList<FSATransition<E>>();	
	
	FSAState<E> start;
	
	public FSA() {
		this.start = addState();
	}
	
	
	public FSAState<E> addState() {
		FSAState<E> state = new FSAState<E>();
		state.number = states.size();
		states.add(state);
		return state;
	}
	
	
	public void addTransition(FSAState<E> from, FSAState<E> to, int label) {
		FSATransition<E> t = from.addTransition(to, label);
		transitions.add(t);		
	}
	


	public void toDot(PrintWriter pw) {
		pw.println("digraph fsa {");
		pw.println("rankdir=LR;");
		pw.println("node [shape=circle]");
		
		for(FSAState<E> s : states) {
			for(FSATransition<E> t : s.transitions) {
				pw.printf("%d -> %d [label=\"%d\"];%n", t.src.number, t.dest.number, t.label);
			}

			if(isFinal(s)) {
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

	/**
	 * Compute lambda closure from given set of states. Lambda-closure
	 * is a set of states that is reachable from given set by epsilon transitions
	 * 
	 * @param states initial set of states
	 * 
	 * @return lambda-closed states
	 */
	public Set<FSAState<E>> lambdaClosure(Set<FSAState<E>> states) {
		LinkedList<FSAState<E>> list = new LinkedList<FSAState<E>>(states);
		Set<FSAState<E>> closure = new HashSet<FSAState<E>>(states);

		while(!list.isEmpty()) {
			FSAState<E> current = list.removeFirst();
			for(FSATransition<E> t : current.transitions) {
				if(t.isEpsilon()) {
					FSAState<E> target = t.dest;
					if(!closure.contains(target)) {
						closure.add(target);
						list.addFirst(target);
					}
				}
			}
		}
		return closure;
	}
	
	/**
	 * Get transition labels from states
	 * 
	 * @param states set of states
	 * 
	 * @return labels
	 */
	private TIntSet labels(Set<FSAState<E>> states) {
		TIntHashSet set = new TIntHashSet();
		
		for(FSAState<E> s : states) {
			for(FSATransition<E> t : s.transitions) {
				if(!t.isEpsilon())
					set.add(t.label);
			}
		}
		
		return set;
	}
	
	/**
	 * Compute set of next states from given set by specific label 
	 * 
	 * @param states set states 
	 * @param label label 
	 * 
	 * @return
	 */
	private Set<FSAState<E>> next(Set<FSAState<E>> states, int label) {
		Set<FSAState<E>> next = new HashSet<FSAState<E>>();
		
		for(FSAState<E> s : states) {
			for(FSATransition<E> t : s.transitions) {
				if(t.label == label)
					next.add(t.dest);
			}
		}
		
		return next;
	}
	
	/**
	 * Determinize this FSM
	 * @param fsm target FSM
	 */
	public void determinize(FSA<E> fsm) {
		
		
		Map<Set<FSAState<E>>, FSAState<E>> newStates = new HashMap<Set<FSAState<E>>, FSAState<E>>();
		Set<Set<FSAState<E>>> dStates = new HashSet<Set<FSAState<E>>>();
		LinkedList<Set<FSAState<E>>> unmarkedDStates = new LinkedList<Set<FSAState<E>>>();
		
		Set<FSAState<E>> currentDState = new HashSet<FSAState<E>>();


		currentDState.add(start);
		currentDState = lambdaClosure(currentDState);
		dStates.add(currentDState);
		unmarkedDStates.add(currentDState);
		
		newStates.put(currentDState, fsm.start);
		mergeFinals(fsm.start, currentDState);
		
		while(!unmarkedDStates.isEmpty()) {
			currentDState = unmarkedDStates.removeFirst();
			TIntSet labels = labels(currentDState);

			for(int label : labels.toArray()) {
				Set<FSAState<E>> next = next(currentDState, label);
				next = lambdaClosure(next);

				// add new state to deterministic automaton
				if(!dStates.contains(next)) {
					dStates.add(next);
					unmarkedDStates.add(next);
					FSAState<E> newState = fsm.addState();
					newStates.put(next, newState);
					mergeFinals(newState, next);
				}

				FSAState<E> currentState = newStates.get(currentDState);
				FSAState<E> newState = newStates.get(next);
				fsm.addTransition(currentState, newState, label);
			}
		}
	}

	
	/**
	 * Converts this epsilon-NFA to epsilon-free NFA. Non-destructive procedure
	 * 
	 * @param fsm target FSM
	 */
	public void epsilonFreeFSM(FSA<E> fsm) {

		Map<Set<FSAState<E>>, FSAState<E>> newStates = new HashMap<Set<FSAState<E>>, FSAState<E>>();
		Set<Set<FSAState<E>>> dStates = new HashSet<Set<FSAState<E>>>();
		LinkedList<Set<FSAState<E>>> unmarkedDStates = new LinkedList<Set<FSAState<E>>>();
		Set<FSAState<E>> currentDState = new HashSet<FSAState<E>>();


		currentDState.add(start);
		currentDState = lambdaClosure(currentDState);
		dStates.add(currentDState);
		unmarkedDStates.add(currentDState);

		newStates.put(currentDState, fsm.start);

		mergeFinals(fsm.start, currentDState);

		while(!unmarkedDStates.isEmpty()) {
			currentDState = unmarkedDStates.removeFirst();

			for(FSAState<E> state: currentDState) {
				for(FSATransition<E> t : state.transitions) {

					// skip epsilon transitions
					if(t.isEpsilon())
						continue;

					FSAState<E> target = t.dest;
					Set<FSAState<E>> newDState = new HashSet<FSAState<E>>();
					newDState.add(target);
					newDState = lambdaClosure(newDState);

					// add new state to epsilon-free automaton
					if(!dStates.contains(newDState)) {
						dStates.add(newDState);
						unmarkedDStates.add(newDState);
						FSAState<E> newState = fsm.addState();
						newStates.put(newDState, newState);
						mergeFinals(newState, newDState);
					}

					FSAState<E> currentState = newStates.get(currentDState);
					FSAState<E> newState = newStates.get(newDState);
					fsm.addTransition(currentState, newState, t.label);
				}
			}

		}
	}
	
	public FSAState<E> getStart() {
		return start;
	}
	public FSAState<E> getState(int index) {
		return states.get(index);
	}
	
	public int size() {
		return states.size();
	}
	
	/**
	 * Reverse this FSM
	 * 
	 * @param fsm target fsm
	 */
	public void rev(FSA<E> fsm) {
		
		List<FSAState<E>> finals = new ArrayList<FSAState<E>>();

		for(int i = 0; i < states.size(); i++ ) {
			FSAState<E> s = fsm.addState();
			FSAState<E> s0 = states.get(i);

			if(isFinal(s0)) {
				s.finals = s0.finals;
				finals.add(s);
			}
			
		}
		
		
		for(FSATransition<E> t : transitions) {
			FSAState<E> from = fsm.getState(t.dest.getNumber() + 1);
			FSAState<E> to = fsm.getState(t.src.getNumber() + 1);
			fsm.addTransition(from, to, t.label);
		}
		
		for(FSAState<E> f : finals) {
			fsm.addTransition(fsm.start, f, Constants.EPSILON);
		}
		
	}
	
	/**
	 * Compute list of final states
	 * @return
	 */
	public List<FSAState<E>> finals() {
		List<FSAState<E>> finals = new ArrayList<FSAState<E>>();
		
		for(FSAState<E> s : states) {
			if(isFinal(s))
				finals.add(s);
		}
		
		return finals;
	}
	
	
	
	/**
	 * Reverse transitions
	 */
	private void trReverse() {
		for(FSAState<E> s : states) {
			s.transitions.clear();
		}
	
		
		for(FSATransition<E> t : transitions) {
			FSAState<E> temp = t.dest;
			t.dest = t.src;
			t.src = temp;
			
			states.get(t.src.number).transitions.add(t);
		}
		
		
	
	}
	
	/**
	 * Sort transitions
	 */
	private void trSort() {
		Collections.sort(transitions, new Comparator<FSATransition<E>>() {

			@Override
			public int compare(FSATransition<E> o1, FSATransition<E> o2) {
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
			for(FSATransition<E> t : transitions) {
				
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
			
			for(FSATransition<E> t : transitions) {
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
				FSAState<E> s = states.get(state);
				finalties[state] = -1;
				if (isFinal(s)) {
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

	
	/**
	 * Automata minimization using Hopcroft's algorithm
	 */
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
				FSAState<E> s = this.states.get(state);

				for(FSATransition<E> t : s.transitions) {
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
	
	/**
	 * Minimize fsm
	 * 
	 * @param fsm target fsm
	 */
	public void minimize(FSA<E> fsm) {
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
			FSAState<E> s = states.get(state);

			// set start state
			if(s.number == 0) {
				fsm.start = fsm.states.get(i);
			}
			
			for(FSATransition<E> t : s.transitions) {
				fsm.addTransition(fsm.states.get(i), fsm.states.get(data.statesClassNumber[t.dest.number]), t.label);
			}
		}
		
		// add finals
		for(int i = 0; i < data.classesStored; i++) {
			int state = data.classesFirstState[i];
			FSAState<E> s0 = states.get(state);

			FSAState<E> s = fsm.states.get(i);
			
			if(isFinal(s0)) {
				mergeFinals(s, s0);
			}
		}

				
	}

	/**
	 * Merge finals from source state set to the destination set
	 * 
	 * @param dest destination state
	 * @param src source state set
	 */
	public void mergeFinals(FSAState<E> dest, Collection<FSAState<E>> src) {
		for(FSAState<E> s : src) {
			mergeFinals(dest, s);
		}
		
	}
	
	/**
	 * Merge finals from source state to destination state
	 * 
	 * @param dest destination state
	 * @param src source state
	 */
	public void mergeFinals(FSAState<E> dest, FSAState<E> src) {
		
	}
	
	public boolean isFinal(FSAState<E> s) {
		return s.finals != null;
	}
}
