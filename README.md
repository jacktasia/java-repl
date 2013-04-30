java-repl
=========

java-repl is an experimental REPL for Java.

Why?
----

For a really long time I programmed (almost exclusively) in dynamic languages. I loved being able to quickly and easily test stuff in REPLs.
When I started programming a lot in Java I started to miss being able to do this. With java-repl you can quickly and easily grab a jar and code with it
without having to manage a java project.

What about beanshell & other JVM shells?
----------------------------------------

I could never get beanshell to work correctly. I think it's just out of date. As for the shells that other JVM languages have (groovy, scala, etc)
...these probably work just fine for most people, that want dynamic access to JVM libraries, but I like testing EXACTLY what I am trying to do. Additionally, when
stuff fails I like getting the "real" error message. In other words, seeing how stuff breaks is just as important to me as confirming something works.


How is this different from "real" REPLs?
----------------------------------------

Since Java is a complied/static language a traditional REPL just isn't possible. java-repl is essentially just injecting your code into a simple `main` method template.
Then it attempts to compile that file and if that works run it. Yes, that means it's running ALL valid code EVERY time you run a command. This is why the `command` section below 
has a number of features (like `i`, `r`, `runonce`) that are unnecessary with real REPLs. These quickly get old. I discourage too much time spent in java-repl. It's meant for checking
something quickly and moving on...


Requires
--------

* JDK 1.6+ 

Compiling From Source
---------------------

Normal Maven project (compile and run):

`mvn clean compile exec:exec`

Create the java-repl jar (in `target` folder)

`ant`


Running
-------

From the command line:

`java -jar java-repl.jar`

Frome the command line with custom REPL file

`java -jar java-repl.jar myproject.repl`

Then if you like it do something like `alias javarepl='java -jar path/to/java-repl.jar'` in .bashrc / .bash_profile


REPL Files
----------

Running javarepl as shown above will also automatically load the `.javarepl` file if it is in the user's home directory.

So if your `.javarepl` had this:
```
addjar /home/jack/Downloads/guava-11.0.2.jar
import com.google.common.base.Joiner;
```

you could immediately use the Joiner class:
```
java> String[] letters = new String[] { "a", "b", "c" };
java> String someLetters = Joiner.on(",").join(letters);
java> System.out.println(someLetters);
a,b,c
```

The idea is that your `.javarepl` would contain your standard toolkit of code. So you can easily use them when hacking on a problem.

Alternatively, and perhaps more often, you may want a specifc `.repl` file. To use a `.repl`on a per project basis; pass it as the first argument:

`java -jar java-repl.jar myproject.repl`

Commands
--------

<table>
  <tr>
    <th>command</th>
    <th>description</th>
  </tr>
  </tr>
  <tr>
    <td>addjar</td>
    <td>
        add a jar to class path (requires full path)
        <br><br>
        Example: addjar /home/jack/libs/guava.jar
    </td>
  </tr>
  <tr>
    <td>addcp</td>
    <td>
      add a directory to class path (requires full path)
      <br><br>
      Example: addcp /home/jack/some_classes/
    </td>
  </tr>
  <tr>
    <td>clear</td>
    <td>clear the screen</td>
  </tr>
  <tr>
    <td>i:<i>line</i>:<i>code</i></td>
    <td>
      [i]nsert code at line
      <br><br>
      Example: i:14:System.out.println("this becomes the first line");
    </td>
  </tr>
  <tr>
    <td>r:<i>line</i>:<i>code</i></td>
    <td>[r]eplace OR [r]emoves passed line (if no code passed)
        <br><br>
        Example: i:14:System.out.println("this replaces the first line");
    </td>
  </tr>
  <tr>
    <td>run</td>
    <td>re-runs last time (only running validated code)</td>
  </tr>
  <tr>
    <td>addline</td>
    <td>add a line of code for .repl file (same as entering code at prompt)
        <br><br>
        Example: addline i++;
    </td>
  </tr>
  <tr>
    <td>runonce</td>
    <td>add a line of code to RUN ONCE for config file (same as entering code at prompt)
        <br><br>
        Example: runonce i++;
    </td>
  </tr>
</table> 

Tests
-----

Top of my TODO list. Please feel free to fork and add more!