fsa
=====

Finite State Automata.
Initially implemented trie data structure and Jan Daciuk's incremental minimal deterministic acyclic final automata (DAFSA) construction.
Now also contains a port of the generic FSA from GATE package.

The details of the DAFSA consturction algorithm are described in paper:
Jan Daciuk; Bruce W. Watson; Stoyan Mihov; Richard E. Watson "Incremental Construction of Minimal Acyclic Finite-State Automata".
Computational Linguistics Vol.26, Num. 1, 2000.

http://aclweb.org/anthology-new/J/J00/J00-1002.pdf

The base algorithm is implemented as an adaptor over actual FSA implementation, so it allows use of different
state implementations.

Classes are named as follows:

[LabelType]DaciukAlgo[StateType], where

LabelType = {int, long, generic}

StateType = {indexed, object}

StateType represents how a single state of the FSA is accessed:
'indexed' means that the state is accessed by its index (number),

'object' means that the state is accesses by its object

Indexed access could be used for 'stateless' FSA implementation where a state
isn't represented by a Java object

The generic FSM don't follow this convention as determinization/minimization algorithms
constructs a fresh FSM instead of in-place transformation. So it is needed to move 
state final data from one FSM to another.

DAFSA
=====

There is DAFSA implementations. 

The format is following:

[LabelType]DAFSA[FinalType]

Currently there is only one label type implemented: Int - int-based labels.

FinalType could be Int - means a set of ints, and boolean - a flag indicating if the
state is final or not.

There are two styles of automata. The first type is designed to build DAFSAs, they implement
a state as an object, which isn't very compact.

Another format is designed specifically to walking FSAs, the implements FSA as a transition table
with start/end state offsets. This representation is optimized for size and speed. But it doesn't
allow modification of the FSA.

