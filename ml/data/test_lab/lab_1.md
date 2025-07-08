```
4CCS1PPA Programming Practice and Applications
```
# Coursework 3: Predator/Prey Simulation

## Michael Kolling, Josh Murphy, Jeffery Raphael

### Questions? programming@kcl.ac.uk

Your task is to extend a predator/prey simulation. You should use the foxes-and-rabbits-handout
project (presented in the lecture, available for download from KEATS) as a basis for your own
project, and modify and extend it to make it more interesting. Note that this is slightly different
from the book project, so please use the version from KEATS, not from the book projects.

You must replace the Fox and Rabbit classes with different kinds of predator and prey to simulate
a different habitat (for example, under water, in a jungle, or in a fantasy world). You may add
additional classes to those included in the existing project.

This project is a pair programming task. You must work in pairs. Information about pair programming is provided separately. We will not accept submissions by individuals.

## 1 Core tasks

You should aim at completing all core tasks. They are as follows.

- Your simulation should have at least five different kinds of acting species. At least two of
    these should be predators (they eat another species), and at least two of them should not be
    predators (they may eat plants). Plants can either be assumed to always be available (as in
    the original project), or they can be simulated (see below).
- At least two predators should compete for the same food source.
- Some or all of the species should distinguish male and female individuals. For these, the
    creatures can only propagate when a male and female individual meet. (“Meet” means they
    need to be within a specified distance to each other, for example in a neighbouring cell.)
    You will need to experiment with the parameters for breeding probability to create a stable
    population.
- You should keep track of the time of day. At least some creatures should exhibit different
    behavior at some time of the day (for example: they may sleep at night and not move
    during that time).

You should implement the core tasks first before you move on to the extension tasks.


## 2 Challenge tasks

Once you have finished the core tasks, implement one or more extension tasks. You can either
choose from the following suggestions, or invent your own. You will be graded on a maximum of
four extensions.

- Add plants. Plants grow at a given rate, but they do not move. Some creatures eat plants.
    They will die if they do not find their food plant.
- Add weather. Weather can change, and it influences the behaviour of some simulated aspects.
    For example, grass may not grow without rain, or predators cannot see well in fog.
- Add disease. Some animals are occasionally infected. Infection can spread to other animals
    when they meet.

If you invent your own extension tasks, check with your class supervisor before implementing
them. You can get their comments to ensure they your idea is not too simple or too difficult.

To get full marks on this assignment you must demonstrate exceptional technical aptitude with
the challenge tasks you implement.

## 3 Extra work — just for fun

You can extend the GUI (the graphical user interface) itself if you like, but no marks will be
awarded for this work. If you do this — that’s good, but it is purely for fun and for your own
practice.

## 4 Submission and Deadline

The submission consists of two parts: your code and a report documenting it.

### The code

You have to submit a Jar of your project to the “Assignment 3: code submission” link in the
assignment 3 section on the PPA KEATS page, before the due date. Your code will be assessed
for:

- correct use of language constructs;
- commenting (is everything well-documented?);
- style (is the code nicely laid out and formatted, and are the variable names chosen well?);
- design (consideration given to cohesion, coupling, maintainability);
- challenge tasks (extra marks for technically impressive extensions).

### The report

You also need to submit a written report that includes the following.

- A description of your simulation, including the types of species that you are simulating, their
    behavior and interactions.
- A list and description of all extension tasks you have implemented.


- Known bugs or problems (Note: for a bug in your code that you document yourself, you
    may not lose many marks — maybe none, if it is in a challenge task. For bugs that we find
    that you did not document you will probably lose marks.)

The report must be submitted to the “Assignment 2: report submission” link in the assignment 2
section on the PPA KEATS page, before the deadline.

The report should be no more than four pages long.

Marking the assignment will involve an interview in your lab class. More detail about the interview
will be provided separately.

The report must clearly state the names and student numbers of all students who worked on the
submission.

The code and report must be submitted via the assignment 3 submission link on the PPA KEATS
page, before the deadline, byboth members of your pair. Both the source code and the report
should clearly state the names of both authors.

## 5 Mark breakdown

The marks are distributed as follows:
```
4CCS1PPA Programming Practice and Applications
```
# Coursework 3: Predator/Prey Simulation

## Michael Kolling, Josh Murphy, Jeffery Raphael

### Questions? programming@kcl.ac.uk

Your task is to extend a predator/prey simulation. You should use the foxes-and-rabbits-handout
project (presented in the lecture, available for download from KEATS) as a basis for your own
project, and modify and extend it to make it more interesting. Note that this is slightly different
from the book project, so please use the version from KEATS, not from the book projects.

You must replace the Fox and Rabbit classes with different kinds of predator and prey to simulate
a different habitat (for example, under water, in a jungle, or in a fantasy world). You may add
additional classes to those included in the existing project.

This project is a pair programming task. You must work in pairs. Information about pair programming is provided separately. We will not accept submissions by individuals.

## 1 Core tasks

You should aim at completing all core tasks. They are as follows.

- Your simulation should have at least five different kinds of acting species. At least two of
    these should be predators (they eat another species), and at least two of them should not be
    predators (they may eat plants). Plants can either be assumed to always be available (as in
    the original project), or they can be simulated (see below).
- At least two predators should compete for the same food source.
- Some or all of the species should distinguish male and female individuals. For these, the
    creatures can only propagate when a male and female individual meet. (“Meet” means they
    need to be within a specified distance to each other, for example in a neighbouring cell.)
    You will need to experiment with the parameters for breeding probability to create a stable
    population.
- You should keep track of the time of day. At least some creatures should exhibit different
    behavior at some time of the day (for example: they may sleep at night and not move
    during that time).

You should implement the core tasks first before you move on to the extension tasks.


## 2 Challenge tasks

Once you have finished the core tasks, implement one or more extension tasks. You can either
choose from the following suggestions, or invent your own. You will be graded on a maximum of
four extensions.

- Add plants. Plants grow at a given rate, but they do not move. Some creatures eat plants.
    They will die if they do not find their food plant.
- Add weather. Weather can change, and it influences the behaviour of some simulated aspects.
    For example, grass may not grow without rain, or predators cannot see well in fog.
- Add disease. Some animals are occasionally infected. Infection can spread to other animals
    when they meet.

If you invent your own extension tasks, check with your class supervisor before implementing
them. You can get their comments to ensure they your idea is not too simple or too difficult.

To get full marks on this assignment you must demonstrate exceptional technical aptitude with
the challenge tasks you implement.

## 3 Extra work — just for fun

You can extend the GUI (the graphical user interface) itself if you like, but no marks will be
awarded for this work. If you do this — that’s good, but it is purely for fun and for your own
practice.

## 4 Submission and Deadline

The submission consists of two parts: your code and a report documenting it.

### The code

You have to submit a Jar of your project to the “Assignment 3: code submission” link in the
assignment 3 section on the PPA KEATS page, before the due date. Your code will be assessed
for:

- correct use of language constructs;
- commenting (is everything well-documented?);
- style (is the code nicely laid out and formatted, and are the variable names chosen well?);
- design (consideration given to cohesion, coupling, maintainability);
- challenge tasks (extra marks for technically impressive extensions).

### The report

You also need to submit a written report that includes the following.

- A description of your simulation, including the types of species that you are simulating, their
    behavior and interactions.
- A list and description of all extension tasks you have implemented.


- Known bugs or problems (Note: for a bug in your code that you document yourself, you
    may not lose many marks — maybe none, if it is in a challenge task. For bugs that we find
    that you did not document you will probably lose marks.)

The report must be submitted to the “Assignment 2: report submission” link in the assignment 2
section on the PPA KEATS page, before the deadline.

The report should be no more than four pages long.

Marking the assignment will involve an interview in your lab class. More detail about the interview
will be provided separately.

The report must clearly state the names and student numbers of all students who worked on the
submission.

The code and report must be submitted via the assignment 3 submission link on the PPA KEATS
page, before the deadline, byboth members of your pair. Both the source code and the report
should clearly state the names of both authors.

## 5 Mark breakdown

The marks are distributed as follows:

- Report:2 marks. However, if the report does not adequately describe your code you may
    lose marks on that part of your code as well. Not submitting a report will result in 0 marks
    for the entire submission.
- Base tasks4 marks.
- Challenge tasks10 marks.
- Commenting and style4 marks.

## 6 Deadline

This assignment (code and report) is dueFriday 22nd February, 17:00.



- Report:2 marks. However, if the report does not adequately describe your code you may
    lose marks on that part of your code as well. Not submitting a report will result in 0 marks
    for the entire submission.
- Base tasks4 marks.
- Challenge tasks10 marks.
- Commenting and style4 marks.

## 6 Deadline

This assignment (code and report) is dueFriday 22nd February, 17:00.


