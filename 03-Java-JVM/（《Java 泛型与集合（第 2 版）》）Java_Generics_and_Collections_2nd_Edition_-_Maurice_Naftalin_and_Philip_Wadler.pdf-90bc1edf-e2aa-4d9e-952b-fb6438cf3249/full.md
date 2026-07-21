# Java Generics and Collections

Fundamentals and Recommended Practices

![](images/db0154c1670033ec3d808d5ccb47881d8bc28b394e68bbc8a2fb229f4d155ded.jpg)

Early Release

RAW& UNEDITED

Maurice Naftalin & Philip Wadler

# Java Generics and Collections

Fundamentals and Recommended Practices

SECOND EDITION

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

# Maurice Naftalin and Philip Wadler

# O'REILLY?

Beijing·Boston ·Farnham·Sebastopol ·Tokyo

OceanofPDF.com

# Java Generics and Collections

by Maurice Naftalin and Philip Wadler

Copyright $©$ 2023 Morningside Light. All rights reserved.

Printed in the United States of America.

Published by O’Reilly Media, Inc., 1005 Gravenstein Highway North, Sebastopol, CA 95472.

O’Reilly books may be purchased for educational, business, or sales promotional use. Online editions are also available for most titles (http://oreilly.com). For more information, contact our corporate/institutional sales department: 800-998-9938 or corporate@oreilly.com.

Acquisitions Editor: Brian Guerin   
Development Editor: Sara Hunter   
Production Editor: Aleeya Rahman   
Interior Designer: David Futato   
Illustrator: Kate Dullea   
December 2023: Second Edition

# Revision History for the Early Release

2023-04-25: First Release   
2023-06-30: Second Release

See http://oreilly.com/catalog/errata.csp?isbn=9781098136727 for release details.

The O’Reilly logo is a registered trademark of O’Reilly Media, Inc. Java Generics and Collections, the cover image, and related trade dress are

trademarks of O’Reilly Media, Inc.

The views expressed in this work are those of the authors, and do not represent the publisher’s views. While the publisher and the authors have used good faith efforts to ensure that the information and instructions contained in this work are accurate, the publisher and the authors disclaim all responsibility for errors or omissions, including without limitation responsibility for damages resulting from the use of or reliance on this work. Use of the information and instructions contained in this work is at your own risk. If any code samples or other technology this work contains or describes is subject to open source licenses or the intellectual property rights of others, it is your responsibility to ensure that your use thereof complies with such licenses and/or rights.

978-1-098-13666-6

OceanofPDF.com

# Dedication

We dedicate this book to Joyce Naftalin, Lionel Naftalin, Adam Wadler, and Leora Wadler

—Maurice Naftalin and Philip Wadler

OceanofPDF.com

# Chapter 1. Subtyping and Wildcards

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 2nd chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

Now that we’ve covered the basics, we can start to cover more advanced features of generics, such as subtyping and wildcards. In this section, we’ll review how subtyping works and we’ll see how wildcards let you use subtyping in connection with generics. The Java Collections Framework will serve as the source of our examples; see Part 2 for detail on specific features of the Collections API.

# Subtyping and the Substitution Principle

Subtyping is a key feature of object-oriented languages such as Java. In Java, one type is a subtype of another if they are related by an extends or implements clause. Here are some examples:

<table><tr><td>Integer</td><td>is a subtype of</td><td>Number</td></tr><tr><td>Double</td><td>is a subtype of</td><td>Number</td></tr><tr><td>ArrayList&lt;E&gt;</td><td>is a subtype of</td><td>List&lt;E&gt;</td></tr><tr><td>List&lt;E&gt;</td><td>is a subtype of</td><td>Collection&lt;E&gt;</td></tr><tr><td>Collection&lt;E&gt;</td><td>is a subtype of</td><td>Iterator&lt;E&gt;</td></tr></table>

Subtyping is transitive, meaning that if one type is a subtype of a second, and the second is a subtype of a third, then the first is a subtype of the third. So, from the last two lines in the preceding list, it follows that List<E> is a subtype of Iterable<E>. If one type is a subtype of another, we also say that the second is a supertype of the first. Every reference type is a subtype of Object, and Object is a supertype of every reference type. We also define the subtype relationship so that every type is a subtype of itself.

The most important property of subtyping is summarized in the Substitution Principle 1

Substitution Principle: wherever a value of type T is expected, you can provide instead a value of a subtype of T.

For example, a variable of a given type may be assigned a value of any subtype of that type, and a method with a parameter of a given type may be invoked with an argument of any subtype of that type.

Consider the interface Collection<E>. One of its methods is add, which takes a parameter of type E:

interface Collection<E> {

```txt
public boolean add(E elt); 
```

According to the Substitution Principle, if we have a collection of numbers we may add an integer or a double to it, because Integer and Double are subtypes of Number.

```txt
List<Number> nums = new ArrayList<>();  
nums.add(2);  
nums.add(0.25);  
assert nums.equals(List.of(2, 0.25)); 
```

Here, subtyping is used in two ways for each method call. The first call is permitted because nums has the type List<Number>, which is a subtype of Collection<Number>, and 2 has the type Integer (thanks to boxing), which is a subtype of Number. The second call is similarly permitted. In both calls, the E in List<E> is taken to be Number.

It may seem reasonable to expect that since Integer is a subtype of Number, it follows that List<Integer> is a subtype of List<Number>. But this is not the case, because, if it were, the Substitution Principle would rapidly get us into trouble. To see why it is not safe to assign a value of type List<Integer> to a variable of type List<Number>, consider the following code:

```txt
List<Integer> ints = new ArrayList<>();  
List(Number> nums = ints; // ① can't be allowed  
nums.add(3.14); 
```

This code assigns the variable ints to point to a list of Integer, and then—if the Substitution Principle were to require to succeed—assigns nums to point to the same list; in that case, the call in the following line— which is clearly legal—would add a Double to this list, meaning that the variable ints, typed as a List<Integer> would be pointing at a list containing a Double, and Java’s type system would be broken. Obviously, this can’t be allowed; the solution is to outlaw assignment . If

List<Integer> is prevented from being a subtype of List<Number>, then the Substitution Principle does not apply and can be reported as a compile error.

What about the reverse? Can we take List<Number> to be a subtype of List<Integer>? No, that doesn’t work either, as shown by the following code:

```txt
List<Number> nums = new ArrayList<>();  
nums.add(3.14);  
List<Integer> ints = nums; // ② can't be allowed 
```

where allowing leads to the same problem—once again, a variable typed as List<Integer> is pointing to a list containing a Double. So List<Number> is not a subtype of List<Integer>, and since we’ve already seen that List<Integer> is not a subtype of List<Number>, all we have is the trivial case where List<Integer> is a subtype of itself. (We also have the more intuitive relationship that List<Integer> is a subtype of Collection<Integer>.)

Arrays behave quite differently; with them, Integer[] is a subtype of Number[]. We will compare the treatment of lists and arrays later (see “Arrays”).

Sometimes we would like lists to behave more like arrays, in that we want to accept not only a list with elements of a given type, but also a list with elements of any subtype of a given type. For this purpose, we use wildcards.

# Wildcards with extends

Another method in the Collection interface is addAll, which adds all members of one collection to another:

```typescript
interface Collection<E> { public boolean addAll(Collection<? extends E> c); 
```

```txt
1 
```

Clearly, given a collection of elements of type E, we should be able to add all members of another collection with elements of type E. The quizzical phrase “? extends E” means that it is also OK to add all members of a collection with elements of any type that is a subtype of E. The question mark is called a wildcard; it stands for some as-yet-unknown subtype of E.

Here is an example. We create an empty list of numbers, and add to it first a list of integers and then a list of doubles:

```txt
List<Number> nums = new ArrayList<>();  
List<String> ints = List.of(1, 2);  
List<Double> dbs = List.of(1.0, 0.5);  
nums.addAllints);  
nums>All(dbls);  
assert nums.equals(List.of(1, 2, 1.0, 0.5)); 
```

The first call is permitted because nums has type List<Number>, which is a subtype of Collection<Number>, and ints has type List<Integer>, which is a subtype of Collection<Integer>, which is in turn a subtype of Collection<? extends Number>. The second call is similarly permitted. In both calls, E is taken to be Number. If the parameter declaration for addAll had been written without the wildcard, then the calls to add lists of integers and doubles to a list of numbers would not have been permitted; you would only have been able to add a list that was explicitly declared to be a list of numbers.

We can also use wildcards when declaring variables. Here is a variant of the introductory example of the preceding section, changed by adding a wildcard to the second line:

```txt
List<Integer> ints = new ArrayList<>();  
List<? extends Number> ints = ints; // ③ now legal  
ints.add(3.14); // ④ can't be allowed 
```

Previously, line caused a compile-time error (because List<Integer> is not a subtype of List<Number>), but line was fine (because a double is a number, so you can add a double to a List<Number>). Now line is fine (because List<Integer> is a subtype of List<? extends Number>), but line has to be a compile error: you cannot add a double to a List<? extends Number>, since you don’t know the type represented by the wildcard; all you know is that it is some subtype of Number. Otherwise, as before, the last line would result in the variable ints, typed as a List<Integer>, pointing to a list containing a double.

In general, if a structure contains elements with a type of the form “? extends E”, we can get elements out of the structure, but we cannot put elements into the structure. To put elements into the structure we need another kind of wildcard, as explained in the next section.

# Wildcards with super

Here is a method, from the convenience class Collections, that copies the elements from a source list into a destination list:

```java
public static<T> void copy(List<? super T> dst, List<? extends T> src) { for (int i = 0; i < src.size(); i++) { dst.set(i, src.get(i)); } } 
```

The quizzical phrase “? super T” means that the destination list may have elements of any type that is a supertype of T, just as the source list may have elements of any type that is a subtype of T.

Here is a sample call.

List<object>objs = Stream.of(2, 3.14, "four").collect(CollectionstoList()); ListInteger>ints $=$ List.of(5,6);

```javascript
Collections.copy(objs, ints); assert objs.equals(List.of(5, 6, "four")); 
```

As with any generic method, the type parameter may be inferred or may be given explicitly. In this case, there are four possible choices, all of which type-check and all of which have the same effect:

```txt
Collections.copy(objs, ints);  
Collections.<Object>copy(objs, ints);  
Collections.<Number>copy(objs, ints);  
Collections.<Integer>copy(objs, ints); 
```

The first call leaves the type parameter implicit; it is taken to be Integer, since that is the most specific choice that works. In the third line, it is taken to be Number. The call is permitted because objs has type List<Object>, which is a subtype of List<? super Number> (since Object is a supertype of Number, as required by the super wildcard) and ints has type List<Integer>, which is a subtype of List<? extends Number> (since Integer is a subtype of Number, as required by the extends wildcard).

We could also declare the method with several possible signatures (a method signature is the combination of the method identifier, its type parameters, and the types and order of its parameters).

```java
public static <T> void copy(List<T> dst, List<T> src)  
public static <T> void copy(List<T> dst, List<? extends T> src)  
public static <T> void copy(List<? super T> dst, List<T> src)  
public static <T> void copy(List<? super T> dst, List<? extends T> src) 
```

The first of these is too restrictive, as it only permits calls when the destination and source have exactly the same type. The remaining three are equivalent for calls that use implicit type parameters, but differ for explicit type parameters. For the example calls above, the second signature works only when the type parameter is Object, the third signature works only when the type parameter is Integer, and the last signature works (as we

have seen) for all three type parameters—i.e., Object, Number, and Integer. When writing a method signature, always use wildcards where you can, since this permits the widest range of calls.

# The Get and Put Principle

It may be good practice to insert wildcards whenever possible, but how do you decide which wildcard to use? Where should you use extends, where should you use super, and where is it inappropriate to use a wildcard at all?

Fortunately, a simple principle determines which is appropriate.

The Get and Put Principle: use an extends wildcard when you only get values out of a structure, use a super wildcard when you only put values into a structure, and don’t use a wildcard when you both get and put.

In Effective Java ([Bloch17]), Joshua Bloch gives this principle the mnemonic PECS (producer extends, consumer super). We already saw this principle at work in the signature of the copy method:

```txt
public static <T> void copy(List<? super T> dst, List<? extends T> src) 
```

The method gets values out of the source src, so it is declared with an extends wildcard, and it puts values into the destination dst, so it is declared with a super wildcard.

Whenever you use an iterator or a stream, you are getting values out of a structure, so you must use an extends wildcard. Here is the iterator version of a method that takes a collection of numbers, converts each to a double, and sums them up:

```java
public static double sum(Collection<? extends Number> nums) { double s = 0.0; for (Number num : nums) s += num.doubleValue(); 
```

```txt
return s; } 
```

The stream equivalent doesn’t declare a Number variable explicitly, but the type constraint on the argument is exactly the same:

```txt
public static double sum(Collection<? extends Number> nums) { return nums.stream().mapToDouble(Number::doubleValue).sum(); } 
```

Since these methods use extends, the following calls are all legal:

```txt
List<Integer> ints = List.of(1,2,3); assert sum(ints) == 6.0;   
List<Double> doubles = List.of(2.5,3.5); assert sum(doubles) == 6.0;   
List<Number> nums = List.of(1,2,2.5,3.5); assert sum(numbers) == 9.0; 
```

The first two calls would not be legal if extends was not used.

Whenever you use the add method, you’re putting values into a structure, so you should use a super wildcard. Here is a method that takes a collection of numbers and an integer n, and puts the first n integers, starting from zero, into the collection:

```txt
public static void count(Collection<? super Integer> ints, int n) { for (int i = 0; i < n; i++) ints.add(i); } 
```

(There is no simple stream-based equivalent to this method.) Since the parameter to this method uses super, the following calls are all legal:

```java
List<Integer> ints1 = new ArrayList<>(); countints1, 5); assert ints1.equals(List.of(0, 1, 2, 3, 4)); List(Number> nums1 = new ArrayList<>(); 
```

```javascript
count(nums1, 5); nums1.add(5.0); assert nums1.equals(List.of(0, 1, 2, 3, 4, 5.0)); List<object> obj1 = new ArrayList<>(); count(objs1, 5); obj1.add("five"); assert obj1.equals(List.of(0, 1, 2, 3, 4, "five")); 
```

The last two calls would not be legal if super was not used.

Whenever you both put values into and get values out of the same structure, you should not use a wildcard.

```txt
public static double sumCount(Collection<Number> nums, int n) { count nums, n); return sum(numbers); } 
```

The collection is passed to both sum and count, so its element type must both extend Number (as sum requires) and be a supertype of Integer (as count requires). The only two classes that satisfy both of these constraints are Number and Integer; we have picked the first of these. Here is a sample call:

```txt
List<Number> nums2 = new ArrayList<>(); double sum = sumCount nums2,5); assert sum == 10; 
```

Since there is no wildcard, the argument must be a collection of Number.

If you don’t like having to choose between Number and Integer, it might occur to you that if Java let you write a wildcard with both extends and super, you would not need to choose. For instance, we could write the following:

```txt
double sumCount(Collection<? extends Number super Integer> coll, int n) // not legal Java! 
```

Then we could call sumCount on either a collection of numbers or a collection of integers. But Java doesn’t permit this. The only reason for outlawing it is simplicity, and conceivably Java might support such notation in the future. But, for now, if you need to both get and put then don’t use wildcards.

The Get and Put Principle also works the other way around. If an extends wildcard is present, pretty much all you will be able to do is get but not put values of that type; and if a super wildcard is present, pretty much all you will be able to do is put but not get values of that type. For example, consider the following code, which uses a list declared with an extends wildcard:

```txt
List<String> ints = new ArrayList<>();  
ints.add(1);  
ints.add(2);  
List<? extends Number> nums = ints;  
double dbl = sum(numbers); // ok  
nums.add(3.14); // compile-time error 
```

The call to sum is fine, because it gets values from the list, but the call to add is not, because it puts a value into the list. This is just as well, since otherwise we could add a double to a list of integers!

Conversely, consider the following code, which uses a list declared with a super wildcard:

```txt
List<object>objs = new ArrayList<>();  
objs.add(1);  
objs.add("two");  
List<? super Integer> ints = objs;  
ints.add(3); // ok  
double dbl = sum(ints); // compile-time error 
```

Now the call to add is fine, because it puts a value into the list, but the call to sum is not, because it gets a value from the list. This is just as well, because the sum of a list containing a string makes no sense!

The exception proves the rule, as the saying goes, and indeed each of these rules has an exception. The one value that you can put into a type declared with an extends wildcard is null, the only value that belongs to every reference type:

```dart
List<String> ints = new ArrayList<>();  
ints.add(1);  
ints.add(2);  
List<? extends Number> nums = ints;  
nums.add(null); // ok  
assert nums.equals(Arrays.asList(1, 2, null)); 
```

Similarly, the only values that you can get from a type declared with a super wildcard are those of type Object, which is the supertype of every reference type:

```txt
List<0bject> objs = List.of(1,"two");  
List<? super Integer> ints = objs;  
String str = "";  
for (Object obj : ints) str += obj.toString();  
assert str.equals("1two")); 
```

You may find it helpful to think of ? extends T as being an unknown type in an interval bounded by the type of null below and by T above (where the type of null is a subtype of every reference type). Similarly, you may think of ? super T as being an unknown type in an interval bounded by T below and by Object above (see Figure 1-1).

![](images/b1868c630e13b7e0e977fd95e9ff3f5d2b6d5acd47ec13b35b651d9c405b2e92.jpg)  
Figure 1-1. Bounded types: extends and super

It is tempting to think that an extends wildcard ensures immutability or at least unmodifiability (see “Immutability and Unmodifiability”), but it does not. As we saw earlier, given a list of type List<? extends Number>, you can still add null values to the list. You can also remove list elements (using remove, removeAll, or retainAll) or permute the list (using swap, sort, or shuffle in the convenience class Collections; see “Changing the Order of List Elements”). The problems and advantages of achieving unmodifiability or immutability are explored in “Immutability and Unmodifiability” and further in [Link to Come].

Because String is final and can have no subtypes, you might expect that List<String> is the same type as List<? extends String>. But in fact the former is a subtype of the latter, not the same type, as can be seen

by an application of our principles. The Substitution Principle tells us it is a subtype, because it is fine to pass a value of the former type where the latter is expected. The Get and Put Principle tells us that it is not the same type, because we can add a string to a value of the former type but not the latter.

# Arrays

It is instructive to compare the treatment of lists and arrays in Java, keeping in mind the Substitution Principle and the Get and Put Principle.

In Java, array subtyping is covariant, meaning that type S[] is considered to be a subtype of T[] whenever S is a subtype of T. Consider the following code fragment, which allocates an array of integers, assigns it to a variable typed as a Number array, and then attempts to store a double in the array:

```txt
Integer[] ints = {0};  
Number[] nums = ints;  
nums[0] = 3.14; // array store exception 
```

Something is wrong with this program since, if it were to succeed, the variable ints, typed as an Integer[], would be pointing at an array containing a Double, and once again Java’s type system would be broken. Where is the problem? Since Integer[] is considered a subtype of Number[], according to the Substitution Principle the assignment on the second line must be legal. Instead, the problem is caught at run time on the third line. When an array is allocated (as on the first line), it is tagged with its reified type (a run-time representation of its component type, in this case, Integer), and every time a value is stored in the array (as on the third line), an array store exception is raised if the reified type is not compatible with the assigned value (in this case, a double cannot be stored into an array of Integer).

In contrast, the subtyping relation for generics is invariant, meaning that type List<S> is not considered to be a subtype of List<T>, except in

the trivial case where S and T are identical. Here is a code fragment analogous to the preceding one, with lists replacing arrays:

```txt
List<Integer> ints = Arrays.asList(0);  
List<Number> nums = ints; // compile-time error  
nums.set(0, 3.14); 
```

Since List<Integer> is not considered to be a subtype of List<Number>, the problem is detected on the second line, not the third, and it is detected at compile time, not run time.

Wildcards reintroduce covariant subtyping for generics, in that type List<S> is considered to be a subtype of List<? extends T> when S is a subtype of T. Here is a third variant of the fragment:

```txt
List<String> ints = Arrays.asList(0);  
List<? extends Number> nums = ints;  
nums.set(0, 3.14); // compile-time error 
```

As with arrays, the third line is in error, but, in contrast to arrays, the problem is detected at compile time, not run time. The assignment violates the Get and Put Principle, because you cannot put a value into a type declared with an extends wildcard.

Wildcards also introduce contravariant subtyping for generics, in that type List<S> is considered to be a subtype of List<? super T> when S is a supertype of T (as opposed to a subtype). Arrays do not support contravariant subtyping. For instance, recall that the method count accepted a parameter of type Collection<? super Integer> and filled it with integers. There is no equivalent way to do this with an array, since Java does not permit you to write (? super Integer)[].

Detecting problems at compile time rather than at run time brings two advantages, one minor and one major. The minor advantage is that it is more efficient. The system does not need to carry around a description of the element type at run time, and the system does not need to check against this description every time an assignment into an array is performed. The

major advantage is that a common family of errors is detected by the compiler. This improves every aspect of the program’s life cycle: coding, debugging, testing, and maintenance are all made easier, quicker, and less expensive.

Apart from the fact that typing errors are caught earlier, there are many other reasons to prefer collection classes to arrays. Collections are more flexible than arrays; the only operations supported on arrays are to get or set a component, and the number of elements they can contain is fixed. Collections support many additional operations, including testing for containment, adding and removing elements, and combining two collections. Collections may be lists (where order is significant and elements may be repeated, and further operations are available), sets (where order is not significant and elements may not be repeated), or queues (predominantly used in workflow situations), and a number of representations are available, including arrays, linked lists, trees, and hash tables. Specialised collections are available for situations requiring efficient concurrent access. Finally, although this is not an inherent advantage of collections over arrays, the convenience class Collections offers operations to rotate or shuffle a list, to find the maximum of a collection, to make a collection unmodifiable or synchronized, and others—many more than are provided by the corresponding convenience class Arrays.

Nonetheless, there are situations in which arrays are preferable to collections. In particular, arrays of primitive types are much more efficient than either arrays or collections of reference types, since they don’t involve boxing, and they use less memory with much better spatial locality (see “Performance”). Further, assignments into a primitive array need not check for an array store exception, because arrays of primitive type do not have subtypes. These advantages may lead you to replace collections with arrays in performance-critical code sections. As always, you should measure comparative performance to justify such a design, bearing in mind that future compiler and library design improvements may change the relative performance balance. Finally, in some cases arrays may be preferable for reasons of compatibility.

To summarize, it is better to detect errors at compile time rather than run time, but Java arrays are forced to detect certain errors at run time by the decision to make array subtyping covariant. Was this a good decision? Before the advent of generics, it was absolutely necessary. For instance, look at the following methods, which are used to sort any array or to fill an array with a given value:

public static void sort(Object[] a); public static void fill(Object[] a, Object val);

Thanks to covariance, these methods can be used to sort or fill arrays of any reference type. Without covariance and without generics, there would have been no way to declare methods that apply for all types. With generics, however, covariant arrays became unnecessary: these methods could now have the following signatures, directly stating that they work for all types:

public static <T> void sort(T[] a); public static <T> void fill(T[] a, T val);

In some sense, covariant arrays are an artifact of the lack of generics in earlier versions of Java. Once you have generics, covariant arrays are probably the wrong design choice, and the only reason for retaining them is backward compatibility.

Sections ???–??? discuss inconvenient interactions between generics and arrays. For many purposes, it may be sensible to consider arrays as an obsolete type. We return to this point in [Link to Come].

# Wildcards Versus Type Parameters

The contains method checks whether a collection contains a given object, and its generalization, containsAll, checks whether a collection contains every element of another collection. This section presents two alternative approaches to giving generic signatures for these methods. The

first approach uses wildcards and is the one used in the Java Collections Framework. The second approach uses type parameters.

Wildcards Here are the types that the methods have in Java with generics:

```java
interface Collection<E> {
    ...
    public boolean contains(Object o);
    public boolean containsAll(Collection<?> c);
    ...
} 
```

The first method does not use generics at all! The second method is our first sight of an important abbreviation. The type Collection<?> stands for:

```txt
Collection<? extends Object> 
```

Extending Object is one of the most common uses of wildcards, so it makes sense to provide a short form for writing it.

These methods let us test for membership and containment:

```txt
Object obj = "one";
List<object> objs = List.of("one", 2, 3.5, 4);
List<Integer> ints = List.of(2, 4);
assert objs.contains(obj);
assert objs.containsAll(ints);
assert ! ints.contains(obj);
assert ! ints.containsAll(objs); 
```

The given list of objects contains both the string "one" and the given list of integers, but the given list of integers does not contain the string "one", nor does it contain the given list of objects.

The tests ints.contains(obj) and ints.containsAll(objs) might seem silly. Of course, a list of integers won’t contain an arbitrary object, such as the string "one". But it is permitted because sometimes such tests might succeed:

```txt
Object obj = 1;  
List<object> objs = List.of(1, 3);  
List<Integer> ints = List.of(1, 2, 3, 4);  
assert ints.contains(obj);  
assert ints.containsAll(objs); 
```

In this case, the object may be contained in the list of integers because it happens to be an integer, and the list of objects may be contained within the list of integers because every object in the list happens to be an integer.

Type Parameters You might reasonably choose an alternative design for collections—a design in which you can only test containment for subtypes of the element type:

```java
interface MyCollection<E> { // alternative design
...
public boolean contains(E o);
public boolean containsAll(Collection<? extends E> c);
...
} 
```

Say we have a class MyList that implements MyCollection. Now the tests are legal only one way around:

```txt
Object obj = "one";
MyList<object> objs = MyList.of("one", 2, 3.5, 4);
MyList<Integer> ints = MyList.of(2, 4);
assert objs.contains(obj);
assert objs.containsAll(ints);
assert ! ints.contains(obj); // compile-time error
assert ! ints.containsAll(objs); // compile-time error 
```

The last two tests are illegal, because the type declarations now require that we can only test whether a list contains an element of a subtype of that list. So we can check whether a list of objects contains a list of integers, but not the other way around.

The library designers chose wildcards over type parameters in the case of contains, containsAll, and other methods in Collection, List,

and Map that test for, or remove values from, the collection. There are arguments both for and against this choice: we will explore them in ???.

# Wildcard Capture

When a generic method is invoked, the type parameter may be chosen to match the unknown type represented by a wildcard. This is called wildcard capture.

Consider the method reverse in the convenience class

java.util.Collections, which accepts a list of any type and reverses it. It can be given either of the following two signatures, which are equivalent:

```java
public static void reverse(List<T> list);  
public static <T> void reverse(List<T> list); 
```

The wildcard signature is slightly shorter and clearer, and is the one used in the library.

If you use the second signature, it is easy to implement the method:

```java
public static<T> void reverse(List<T> list) { List<T> tmp = new ArrayList<> (list); for (int i = 0; i < list.size(); i++) { list.set(i, tmp.get(list.size()-i-1)); } } 
```

This copies the argument into a temporary list, and then writes from the copy back into the original in reverse order.

If you try to use the first signature with a similar method body, it won’t work:

```java
public static void reverse(List<?> list) { List<object> tmp = new ArrayList<> (list); for (int i = 0; i < list.size(); i++) { list.set(i, tmp.get(list.size()-i-1)); // compile-time error 
```

```txt
} 
```

Now it is not legal to write from the copy back into the original, because we are trying to write from a list of objects into a list of unknown type.

Replacing List<Object> with List<?> won’t fix the problem, because now we have two lists with (possibly different) unknown element types.

Instead, you can implement the method with the first signature by implementing a private method with the second signature, and calling the second from the first:

```java
public static void reverse(List<T> list) { rev(list); } private static <T> void rev(List<T> list) { List<T> tmp = new ArrayList<T>(list); for (int i = 0; i < list.size(); i++) { list.set(i, tmp.get(list.size()-i-1)); } } 
```

Here we say that the type variable T has captured the wildcard. This is a generally useful technique when dealing with wildcards, and it is worth knowing.

Another reason to know about wildcard capture is that it can show up in error messages, even if you don’t use the above technique. In general, each occurrence of a wildcard is taken to stand for some unknown type. If the compiler prints an error message containing this type, it is referred to as capture of ?. For instance, with the OpenJDK compiler at Java 19, the incorrect version of reverse generates the following error message:

Capture.java:8: error: incompatible types: Object cannot be converted to CAP#1 list.set(i, tmp.get(list.size()-i-1)); // compile-time error $\wedge$ where CAP#1 is a fresh type-variable: CAP#1 extends Object from capture of ?

CAP#1 is the name that the compiler has given to the type of the elements of list. If there is more than one distinct wildcard, each represents a different unknown type, so they will be assigned different names—even if the type associated with each is the same, for example capture of ? or, in the case of a bounded wildcard, capture of ? extends Number.

# Restrictions on Wildcards

Wildcards may not appear at the top level in class instance creation expressions (new), in explicit type parameters in generic method calls, or in supertypes (extends and implements).

Instance Creation In a class instance creation expression, if the type is a parameterized type, then none of the type parameters may be wildcards. For example, the following are illegal:

```dart
List<> list = new ArrayList<>(); // compile-time error  
Map<String, ? extends Number> map = new HashMap<String, ? extends Number>(); // compile-time error 
```

This is usually not a hardship. The Get and Put Principle tells us that if a structure contains a wildcard, we should only get values out of it (if it is an extends wildcard) or only put values into it (if it is a super wildcard). For a structure to be useful, we must do both. Therefore, we usually create a structure at a precise type, even if we use wildcard types to put values into or get values from the structure, as in the following example:

```cpp
List<Number> nums = new ArrayList<Number>();  
List<? super Number> sink = nums;  
List<? extends Number> source = nums;  
for (int i=0; i<4; i++) sink.add(i);  
int sum = nums.stream().mapToInt(Number::intValue).sum();  
assert sum == 6; 
```

Here wildcards appear in the second and third lines, but not in the first line that creates the list.

Only top-level parameters in instance creation are prohibited from containing wildcards. Nested wildcards are permitted. Hence, the following is legal:

List<List $<  ? >   >$ lists $\equiv$ new ArrayList<List $<  ? >   >$ (）;   
lists.add(List.of(1,2,3));   
lists.add(List.of("four","five"));   
assert lists.equals(List.of(List.of(1，2，3)，List.of("four", "five")));   
assert lists.getLast().getFirst().toString().equals("1");

Even though the list of lists is created at a wildcard type, each individual list within it has a specific type: the first is a list of integers and the second is a list of strings. The wildcard type prohibits us from extracting elements from the inner lists as any type other than Object, but since that is the type used by toString, this code is well typed.

One way to remember the restriction is that the relationship between wildcards and ordinary types is similar to the relationship between interfaces and classes—wildcards and interfaces are more general, ordinary types and classes are more specific, and instance creation requires the more specific information. Consider the following three statements:

```txt
List<> list = new ArrayList<0bject>(); // ok  
List<> list = new List<0bject>(); // compile-time error  
List<> list = new ArrayList<>(); // compile-time error 
```

The first is legal; the second is illegal because an instance creation expression requires a class, not an interface; and the third is illegal because an instance creation expression requires an ordinary type, not a wildcard.

You might wonder why this restriction is necessary. The Java designers had in mind that every wildcard type is shorthand for some ordinary type, so they believed that ultimately every object should be created with an ordinary type. It is not clear whether this restriction is necessary, but it is unlikely to be a problem. (We tried hard to contrive a situation in which it was a problem, and we failed!)

Generic Method Calls If a generic method call includes explicit type parameters, those type parameters must not be wildcards. For example, say we have the following generic method:

```txt
class Lists { public static <T> List<T> factory() { return new ArrayList<T>(); } } 
```

You may choose for the type parameters to be inferred, or you may pass an explicit type parameter. Both of the following are legal:

```txt
List<> list = Lists.factory();  
List<> list = Lists.<Object>factory(); 
```

If an explicit type parameter is passed, it must not be a wildcard:

```txt
List<> list = Lists.<?>factory(); // compile-time error 
```

As before, nested wildcards are permitted:

```txt
List<List<?>> list = Lists.<List<?>>factory(); // ok 
```

The motivation for this restriction is similar to the previous one. Again, it is not clear whether it is necessary, but it is unlikely to be a problem.

Supertypes When a class instance is created, it invokes the constructor for its supertype. Hence, any restriction that applies to instance creation must also apply to supertypes. In a class declaration, if the supertype or any superinterface has type parameters, these types must not be wildcards.

For example, this declaration is illegal:

class AnyList extends ArrayList<?> {...} // compile-time error

And so is this:

class AnotherList implements List<?> {...} // compile-time error

But, as before, nested wildcards are permitted:

class NestedList extends ArrayList<List<?>> {...} // ok

The motivation for this restriction is similar to the previous two. As before, it is not clear whether it is necessary, but it is unlikely to be a problem.

1 Liskov87] provides a definition of behavioral subtyping in terms of the ability to substitute values of one type for values of another without any change in program behavior. Applying this definition in reverse gives the design principle often called the Liskov Substitution Principle. We discuss its application to the Collections Framework in Chapter 5 and [Link to Come].

OceanofPDF.com

# Chapter 2. Comparison and Bounds

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 3rd chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

Now that we have the basics, let’s look at some more advanced uses of generics. This chapter describes the interfaces Comparable<T> and Comparator<T>, which are used to support comparison on elements. These interfaces are useful if, for instance, you want to find the maximum element of a collection or to sort a list. Along the way, we will introduce bounds on type variables, an important feature of generics that is particularly useful in combination with the Comparable<T> interface.

# Comparable<T>

The interface Comparable<T> declares a single instance method for comparing one object with another:

interface Comparable<T> { public int compareTo(T o);

The compareTo method returns an integer value that is negative, zero, or positive depending upon whether the receiver—this object—is less than, equal to, or greater than the argument. When a class implements Comparable, the ordering specified by this interface is called the natural ordering for that class.

Typically, an object belonging to a class can only be compared with an object belonging to the same class. For instance, Integer implements Comparable<Integer>:

```c
Integer int0 = 0;  
Integer int1 = 1;  
assert int0 compareTo(int1) < 0; 
```

The comparison returns a negative number, since 0 precedes 1 under numerical ordering. Similarly, String implements Comparable<String>:

```txt
String str0 = "zero";
String str1 = "one";
assert str0比较高(str1) > 0; 
```

This comparison returns a positive number, since "zero" follows "one" under alphabetic ordering.

The type parameter to the interface allows nonsensical comparisons to be caught at compile time:

```txt
Integer i = 0;  
String s = "one";  
assert i compareTo(s) < 0; // compile-time error 
```

You can compare an integer with an integer or a string with a string, but attempting to compare an integer with a string is a compile-time error.

Comparison is not supported between arbitrary numerical types:

```javascript
Number m = Integer.valueOf(2);
Number n = Double.valueOf(3.14);
assert m compareTo(n) < 0; // compile-time error 
```

Here the comparison is illegal, because the Number class does not implement the Comparable interface.

Comparison differs from equality in that it does not accept a null argument. If x is not null, x.equals(null) must return false, while x.compareTo(null) must throw a NullPointerException.

We adapt standard idioms for comparison, writing x.compareTo(y) < 0 instead of $\textsf { x } < \textsf { y }$ , and writing x.compareTo(y) $\qquad < = \ O \ \Theta$ instead of x $\ < \ y$ .

Contract for Comparable The contract for the Comparable<T> interface specifies three properties. The properties are defined using the sign function, which is defined such that sgn(x) returns -1, 0, or 1, depending on whether x is negative, zero, or positive.

First, comparison is anti-symmetric. Reversing the order of arguments reverses the result:

```lisp
sgn(x compareTo(y)) == -sgn(y compareTo(x)) 
```

This generalizes the property for numbers: $\texttt { x } < \texttt { y }$ if and only if $\ y \ > \ \mathsf X$ . It is also required that x.compareTo(y) raises an exception if and only if y.compareTo(x) raises an exception.

Taking x and y to be the same gives us sgn(x.compareTo(x)) == - sgn(x.compareTo(x)). It follows that

```javascript
x compareTo(x) == 0 
```

so comparison is reflexive—that is, every value compares as the same as itself.

Second, comparison is transitive. If one value is smaller than a second, and the second is smaller than a third, then the first is smaller than the third:

```txt
if x compareToTo(y) < 0 and y compareToTo(z) < 0 then x compareToTo(z) < 0 
```

This generalizes the property for numbers: if $\texttt { x } < \texttt { y }$ and y < z then x < z.

Third, comparison is a congruence. If two values compare as the same then they compare the same way with any third value:

```txt
if x compareToTo(y) == 0 then sgn(x compareToTo(z)) == sgn(y compareToTo(z)) 
```

This generalizes the property for numbers: if $\mathrm { ~ \sf ~ X ~ } = = \mathrm { ~ \sf ~ y ~ }$ then $\times ~ < ~ z$ if and only if y < z. Presumably, it is also required that if x.compareTo(y) $\ c = 0$ then x.compareTo(z) raises an exception if and only if y.compareTo(z) raises an exception, although this is not explicitly stated.

Consistent with Equals The contract for Comparable strongly recommends that the compareTo method should be consistent with equals —that is, that two objects should satisfy the equals method if and only if they compare as the same:

```txt
x.equals(y) if and only if x compareTo(y) == 0 
```

Notice that this is a recommendation, not a mandatory part of the contract. But in fact it is the normal case: most classes having a natural ordering do comply with this recommendation. If you are designing a class that implements Comparable, you should not ignore it without good reason. The best-known example in the platform library that contravenes this recommendation is java.math.BigDecimal: two instances of this class representing the same value but with different precisions, for example 4.0 and 4.00, will compare as the same but not satisfy the equals method.

One consequence of this design choice is that values of such a class cannot be reliably stored in an internally ordered collection like NavigableSet or NavigableMap, as we will see in Chapter 8. A discussion of possible reasons for choosing inconsistency with equals, and the wider consequences of this choice, will be found in [Link to Come].

Look Out for This! It’s worth pointing out a subtlety in the definition of comparison. Suppose that you have a series of Event objects, each described by its name and the number of milliseconds at which it occurred before or after the present moment. Here is one way to define the natural order for Event to be the same as the timing order:

```javascript
record Event(String name, int millisecs) implements Comparable<Event> { public int compareTo(Event other) { return this.millisecs < other.millisecs ? -1 : this.millisecs == other.millisecs ? 0 : 1; } } 
```

The conditional expression returns -1, 0, or 1 depending on whether the receiver is less than, equal to, or greater than the argument. At first sight, you might think that the following code would work just as well, since compareTo is permitted to return any negative integer if the receiver is less than the argument, and any positive integer if it is greater:

```txt
record Event(String name, int millisecs) implements Comparable<Event> { public int compareTo(Event other) { // bad implementation - don't do this return this.millisecs - other.millisecs; } } 
```

But if the two values being compared have opposite signs, calculating the difference between them may result in overflow—that is, a value outside

the range that can be stored in an integer, between Integer.MIN_VALUE and Integer.MAX_VALUE. In “Comparator<T>” we will see how the factory methods of Comparator provide concise ways of comparing two objects without encountering this problem.

# Maximum of a Collection

In this section, we show how to use the Comparable interface to find the maximum element in a collection.We begin with a simplified version. The signature of the version actually found in the Collections Framework is more complicated, and later we will see why.

Here is the code to find the maximum element in a nonempty collection, from the class Collections:

```txt
public static <T extends Comparable<T>> T max(Collection<T> coll) { Iterator<? extends T> i = coll.iterator(); T candidate = i.next(); while (i.hasNext()) { T next = i.next(); if (next compareToCandidate) > 0) candidate = next; } return candidate; } 
```

We first saw generic methods that declare new type variables in the signature in [Link to Come]. For instance, the method asList takes an array of type E[] and returns a result of type List<E>, and does so for any type E. Here we have a generic method that declares a bound on the type variable. The method max takes a collection of type Collection<T> and returns a T, and it does this for any type T such that T is a subtype of Comparable<T>.

The highlighted phrase in angle brackets at the beginning of the type signature declares the type variable T, and we say that T is bounded by

Comparable<T>. As with wildcards, bounds for type variables are always indicated by the keyword extends, even when the bound is an interface rather than a class, as is the case here. Unlike wildcards, type variables can only be bounded using extends, not super.

In this case, the bound is recursive, in that the bound on T itself depends upon T. It is even possible to have mutually recursive bounds, such as:

```txt
<T extends C<T,U>, U extends D<T,U>> 
```

An example of mutually recursive bounds appears in [Link to Come].

The method body first obtains an iterator over the collection, then calls the next method to select the first element as a candidate for the maximum; the specification of this method allows it to throw a

NoSuchElementException if it is supplied with an empty collection. It then compares the candidate with each element in the collection, setting the candidate to the element when the element is larger.

The code shown above, from the current (Java 20) version of the JDK, was written before Java 8 introduced streams and static methods on interfaces, which permit a more concise and probably more efficient version, with the same signature but returning a value of type Optional<T> to allow for the case of an empty collection:

```java
public static <T extends Comparable<T>> Optional<T> max(Collection<T> coll) { return coll.stream().max(Comparator.naturalOrder()); } 
```

We will explore the features of Comparator that make this possible in “Comparator<T>”.

When calling the method, T may be chosen to be Integer (since Integer implements Comparable<Integer>) or String (since

String implements Comparable<String>):

List<Integer> ints $=$ Arrays.asList(0,1,2);

assert Collections.max(ints) == 2;

List<String> strs $=$ Arrays.asList("zero","one","two");

assert Collections.max(strs).equals("zero");

But we may not choose T to be Number (since Number does not implement Comparable):

List<Number> nums $=$ Arrays.asList(0,1,2,3.14);

assert Collections.max(nums) $= =$ 3.14; // compile-time error

As expected, here the call to max is illegal.

Declarations for methods should be as general as possible to maximize utility. If you can replace a type parameter with a wildcard then you should do so. We can improve the declaration of max by replacing:

<T extends Comparable<T>> T max(Collection<T> coll)

with:

<T extends Comparable<? super T>> T max(Collection<? extends T> coll)

Following the Get and Put Principle, we use extends with Collection because we get values of type T from the collection, and we use super with Comparable because we put value of type T into the compareTo method. In the next section, we’ll see an example that would not type-check if the super clause above was omitted.

If you look at the signature of this method in the Java library, you will see something that looks even worse than the preceding code:

<T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)

The highlighted bound is there for backward compatibility, as we will explain at the end of “Multiple Bounds”.

# A Fruity Example

The Comparable<T> interface gives fine control over what can and cannot be compared. Say that we have a Fruit class with subclasses Apple and Orange. Depending on how we set things up, we may prohibit comparison of apples with oranges or we may permit such comparison.

Example 2-1 prohibits comparison of apples with oranges. Here are the three classes it declares:

```txt
class Fruit {...}
class Apple extends Fruit implements Comparable<Apple> {...}
class Orange extends Fruit implements Comparable<Orange> {...} 
```

Each fruit has a name and a size, and two fruits are equal if they have the same name and the same size. Since we have overridden equals, we have also overridden hashCode, to ensure that equal objects have the same hash code (see Hash Tables for an explanation of the importance of this). Apples are compared by comparing their sizes, and so are oranges. Since Apple implements Comparable<Apple>, it is clear that you can compare apples with apples, but not with oranges. The test code builds three lists, one of apples, one of oranges, and one containing mixed fruits. We may find the maximum of the first two lists, but attempting to find the maximum of the mixed list signals an error at compile time.

Example 2-2 permits comparison of apples with oranges. Compare these three class declarations with those given previously (all differences between Example 2-1 and Example 2-2 are highlighted):

```txt
class Fruit implements Comparable<Fruit> {...}   
class Apple extends Fruit {...}   
class Orange extends Fruit {...} 
```

As before, each fruit has a name and a size, and two fruits are equal if they have the same name and the same size. Now, since Fruit implements Comparable<Fruit>, any two fruits may be compared by comparing their sizes. So the test code can find the maximum of all three lists, including the one that mixes apples with oranges.

Recall that at the end of the previous section we extended the type signature of max to use super:

```java
<T extends Comparable<? super T>> T max(Collection<? extends T> coll) 
```

Example 2-2 shows why this wildcard is needed. If we want to compare two oranges, we take T in the preceding code to be Orange:

```txt
Orange extends Comparable<? super Orange> 
```

And this is true because both of the following hold:

```txt
Orange extends Comparable<Fruit> and Fruit super Orange 
```

Without the super wildcard, finding the maximum of a List<Orange> would be illegal, even though finding the maximum of a List<Fruit> is permitted.

Also note that the natural ordering used here is not consistent with equals (see “Comparable<T>”). Two fruits with different names but the same size compare as the same, but they are not equal.

# Comparator<T>

Sometimes we want to compare objects that do not implement the Comparable interface, or to compare objects using a different ordering from the one specified by that interface. The ordering provided by the Comparable interface is called the natural ordering, so the Comparator interface provides, so to speak, an unnatural ordering.

We specify additional orderings using the Comparator interface, which declares two abstract methods:

```txt
interface Comparator<T> {
    public int compare(T o1, T o2);
    public boolean equals(Object obj);
} 
```

The compare method returns a value that is negative, zero, or positive depending upon whether the first object is less than, equal to, or greater than the second object—just as with compareTo. (The equals method is the one familiar from class Object; it is included in the interface to remind implementors that equal comparators must have compare methods that impose the same ordering.)

Example 2-1. Prohibiting comparison of apples with oranges   
```java
abstract class Fruit {
    protected String name;
    protected int size;
    protected Fruit(String name, int size) {
        this.name = name; this.size = size;
    }
    public boolean equals(Object o) {
        if (o instanceof Fruit) {
            Fruit that = (Fruit)o;
            return this.name.equals(that.name) && this.size == that.size;
        } else return false;
    }
    public int hashCode() {
        return Objects.hashCode(name, size);
    }
    protected int compareTo(Fruit that) {
        return Integer compareTo(this.size, that.size);
    }
} class Apple extends Fruit implements Comparable<Apple> {
    public Apple(int size) { super("Apple", size); }
    public int compareTo(Apple a) { return super compareTo(a); }
} class Orange extends Fruit implements Comparable<Orange> {
    public Orange(int size) { super("Orange", size); }
    public int compareTo(Omega o) { return super compareTo(o); }
} 
```

Example 2-2. Permitting comparison of apples with oranges   
}   
class Test { public static void main(String[] args) { Apple a1 $=$ new Apple(1);Apple a2 $=$ new Apple(2); Orange o3 $=$ new Orange(3); Orange o4 $=$ new Orange(4) List<Apple> apples $=$ Arrays.asList(a1,a2); assert Collections.max(apples).equals(a2); List<Orange> oranges $=$ Arrays.asList(o3,o4); assert Collections.max(oranges).equals(o4); List<Fruit> mixed $=$ List.of(a1,o3); Collections.max(mixed); // compile-time error }   
}

```java
abstract class Fruit implements Comparable<Fruit> {
    protected String name;
    protected int size;
    protected Fruit(String name, int size) {
        this.name = name; this.size = size;
    }
    public boolean equals(Object o) {
        if (o instanceof Fruit) {
            Fruit that = (Fruit)o;
            return this.name.equals(that.name) && this.size == that.size;
        } else return false;
    }
    public int hashCode() {
        return Objects.hashCode(name, size);
    }
    public int compareTo(Fruit that) {
        return Integer compareTo(this.size, that.size);
    }
}  
class Apple extends Fruit {
    public Apple(int size) { super("Apple", size); }
}  
class Orange extends Fruit {
    public Orange(int size) { super("Orange", size); }
}  
class Test {
    public static void main(String[] args) {
        System.out.println("Fruit");
    }
}; 
```

```java
Apple a1 = new Apple(1); Apple a2 = new Apple(2); Orange o3 = new Orange(3); Orange o4 = new Orange(4); List<Apple> apples = Arrays.asList(a1, a2); assert Collections.max(apples).equals(a2); List<Orange> oranges = Arrays.asList(o3, o4); assert Collections.max(oranges).equals(o4); List<Fruit> mixed = List.of(a1, o3); assert Collections.max(mixed).equals(o3); // ok } } 
```

Here is a comparator that considers the shorter of two strings to be smaller. Only if two strings have the same length are they compared using the natural (alphabetic) ordering.

```java
Comparator<String> sizeOrder = new Comparator<> () { public int compare(String s1, String s2) { return s1.length() < s2.length() ? -1 : s1.length() > s2.length() ? 1 : s1 compareTo(s2) ; } }; 
```

And here is an example of its use:

```txt
assert "two".compareTo("three") > 0;  
assert sizeOrder compareTo("two","three") < 0; 
```

In the natural alphabetic ordering, "two" is greater than "three", whereas in the size ordering it is smaller.

The Java libraries always provide a choice between Comparable and Comparator. For every generic method with a type variable bounded by Comparable, there is another generic method with an additional argument of type Comparator. For instance, corresponding to:

```java
public static <T extends Comparable<? super T>> T max(Collection<? extends T> coll) 
```

we also have:

public static $<\text{T}>$ T max(Collection<? extends T> coll, Comparator<? super T> comp)

There are similar methods to find the minimum. For example, here is how to find the maximum and minimum of a list using the natural ordering and using the size ordering:

```txt
Collection<String> strings =   
Arrays.asList("from","aaa","to","zzz");   
assert Collections.maxstrings).equals("zzz");   
assert Collections.minstrings).equals("aaa");   
assert Collections.maxstrings,sizeOrder).equals("from");   
assert Collections.minstrings,sizeOrder).equals("to"); 
```

The string "from" is the maximum using the size ordering because it is longest, and "to" is minimum because it is shortest.

Here, slightly simplified, is the code in the class Collection for the version of max that takes a Comparator.

```java
public static <T extends Comparable<T>> T max(Collection<T> coll, Comparator<? super T> comp) {
    Iterator<? extends T> i = coll.iterator();
    T candidate = i.next();
    while (i.hasNext()) {
        T next = i.next();
        if (compcompare(next, candidate) > 0)
            candidate = next;
    }
    return candidate;
} 
```

Compared to the previous version, the only change is that where before we wrote next.compareTo(candidate), now we write comp.compare(next, candidate).

It is easy to define a comparator that provides the natural ordering. This is a slightly simplified version of the code for the static method naturalOrder on the Comparator interface:

```txt
public static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
return new Comparator<>(){
public int compare(T o1, T o2) { return o1 compareTo(o2); }
};
} 
```

Using this, we can define the version of max that uses the natural ordering in terms of the version that uses a given comparator. This is the overload of max that was used in the stream version of the Collections method in “Comparable ${ < } \mathrm { T } > ^ { \dprime }$ :

```java
public static <T extends Comparable<? super T>> T  
max(Collection<? extends T> coll) {  
    return Collections.max(col1, Comparator.naturalOrder());  
} 
```

In everyday practice, you will probably create Comparator instances using one of the static or default methods that the interface exposes, or a combination of them. Let’s explore how they work on a simple record type:

record Person(String name, int age) {}

Probably the most commonly used methods are comparing and its variants, which accept a function that extracts a key to use in comparing two objects. For example, if we want to sort a list of Person objects by name, we can use this Comparator:

Comparator<Person> compareByName = Comparator.comparing(Person::name);

The method comparing takes a function (represented by the functional interface java.util.function.Function1  —typically a getter—

from the type to be sorted to a sort key. It then returns a comparator that, when it is applied to two objects of the type to be sorted, will return a value corresponding to the natural ordering on their respective sort keys. For example, if we define a list of Person objects:

```dart
Person a32 = new Person("Alice", 32);  
Person b23 = new Person("Bob", 23);  
List<Person> l = new ArrayList<> (List.of(a32, b23)); 
```

we can write

```javascript
1.sort(compareByName); assert 1.equals(List.of(a32,b23)); 
```

Even somewhat simplified, the declaration of Comparator.comparing in the JDK looks very difficult:

public static $<T, U$ extends Comparable<? super U>> Comparator<T> comparing(Function<? super T, ? extends U> keyExtractor) { return (c1, c2) ->   
keyExtractor.apply(c1).compareTo(keyExtractor.apply(c2));   
}

In fact, we have met most of the difficulties already. T is the type of the class to be compared, and U is the type of the sort key. As we saw at the end of “Maximum of a Collection”, the most generally useful bound on the type of a comparable is U extends Comparable<? super U>: in other words, if any supertype of U defines compareTo, U will inherit that definition. The type signature of the Function interface can be explained by the Substitution Principle: a function that can be applied to some supertype of T can certainly be applied to a T value; conversely, if it returns a subtype of U, that will be certainly be usable where a U is required.

The intimidating appearance of this declaration contrasts with the ease of using the method that it declares. This contrast is typical of Java generics: the API designer needs to think very carefully about how to make it as

usable and general as possible. If the design is successful, client code developers will be highly appreciative—if they stop to think about it, that is, since the best-designed APIs are distinguished by their unobtrusiveness.

The Function parameter of the method comparing can only be applied to an object of a reference type. So for primitive types, we need specialized versions of comparing. For example:

```txt
Comparator<Person> compareByAge = Comparator.comparingInt(Person::age);  
1.sort(compareByAge);  
assert 1.equals(List.of(b23, a32)); 
```

Earlier, in “Comparable $< \mathrm { T } > ^ { \dprime \dprime }$ , we saw two solutions to the problem of comparing two Event objects, one clumsy and one defective. We can use comparingInt to define a version of compareTo that is more readable and less error-prone than the first version, but without the overflow problem of the second:

```txt
record Event(String name, int milliseconds) implements Comparable event{ public int compareTo(Event other){ return Comparator.comparingInt(Event::millisecs).compare(this,other); }   
} 
```

The ordering of a comparator can be conveniently reversed using the instance method reversed:

```javascript
1.sort(compareByAge.reversed()); assert 1.equals(List.of(a32,b23)); 
```

Sorting often requires multiple levels: when the result of sorting on one key produces groups of results, with the results in each group having the same value of the sort key. Each group may then need to be sorted by a secondary key. Comparator supports this with the instance method

comparingAndThen, which creates a comparator that compares its arguments first using the receiver (this comparator) then, if they are equal, using the comparator supplied as the argument. To show this in action, we add a new Person object with the same name as one pre-existing object and the same age as the other.

```txt
Person a23 = new Person("Alice", 23);
1.add(a23);
1.sort(compareByName.thenComparing(compareByAge));
assert 1.equals(List.of(a23, a32, b23)); 
```

The Comparator API is designed so that these methods are composable. For example, supposing that we want to sort a list of Person objects in reverse order of age and then, in the case of tied results, by reverse order of name. That is easy to express:

```txt
1.sort(compareByAge.reversed().thenComparing(compareByName.reversed()));   
assert 1.equals(List.of(a32,b23,a23)); 
```

Unlike Comparable::compareTo, the compare method of Comparator can optionally permit comparison of null arguments. The static methods nullsFirst and nullsLast take a Comparator and return a null-friendly comparator that treats non-null values the same as the comparator supplied as the argument, but considers null to be less—or, respectively, greater—than any non-null value:

```javascript
1.add(null);   
1.sort(Comparator nullsFirst(compareByAge));   
assert 1.equals(Arrays.asList(null,b23,a23,a32)); 
```

As a final example of comparators, here is a method that takes a comparator on elements and returns a comparator on lists of elements:

```java
public static <E> Comparator<List<E>> listComparator(final Comparator<? super E> comp) { return new Comparator>(); 
```

```txt
public int compare(List<E> list1, List<E> list2) {  
    int n1 = list1.size();  
    int n2 = list2.size();  
    for (int i = 0; i < Math.min(n1, n2); i++) {  
        int k = comp(compare(list1.get(i), list2.get(i));  
        if (k != 0) return k;  
    }  
    return Integer.compare(n1, n2);  
}; 
```

The loop compares corresponding elements of the two lists, and terminates when corresponding elements are found that are not equal (in which case, the list with the smaller element is considered smaller) or when the end of either list is reached (in which case, the shorter list is considered smaller). This is the usual ordering for lists; if we convert a string to a list of characters, it gives the usual ordering on strings.

# Enumerated Types

Java 5 includes support for enumerated types: types whose values consist of a fixed set of constants. Here are two simple examples:

```txt
enum Season { WINTER, SPRING, SUMMER, FALL }  
enum Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY } 
```

Each enumerated type declaration can be expanded into a corresponding class in a stylized way. The corresponding class is designed so that it has exactly one instance for each of the enumerated constants, bound to a suitable static final variable. For example, the first of the enum declarations above expands into a class called Season. Exactly four instances of this class exist, bound to four static final variables with the names WINTER, SPRING, SUMMER, and FALL. There is no way to create any further instances of Season.

Foremost amongst the many useful features of Java enums is their type safety: for example, you can’t assign a Weekday value to a variable of type Season, or compare two values of these different types. This type safety is enforced by the declaration of the enumerated types, as in Example 2-4. Each class corresponding to an enumerated type is a subclass of java.lang.Enum, declared as in Example 2-32 .

The class Enum provides properties that every enumerated type needs: its name and its ordinal—that is, its position in the enumeration sequence. Also, by implementing Comparable, it ensures that its derived classes will also be Comparable. But the requirement for type safety means that it is not enough that, for example, Season implements Comparable; on its own, that would still permit comparison of a Season with a Weekday. What is necessary is that Season should implement Comparable<Season>.

This takes us some distance towards understanding the declaration of Enum:

class Enum<E extends Enum<E>> implements Comparable<E>

You may find this frightening at first sight—your authors certainly did! But don’t panic. Matching things up shows how a class derived from Enum will have the property that we need. From what we already know, we have that

class Season extends ... implements Comparable<Season>

where ... is some parameterization of Enum. If that parameterization is Enum<Season>, we get

class Season extends Enum<Season> // 1

class Enum<Season> implements Comparable<Season> // 2

and substituting //1 into //2 gives

```txt
class Enum<Season extends Enum<Season>> implements Comparable<Season> 
```

Of course, Enum needs to work on any enumerated type, not only Season, so its declaration in the class library is

```txt
class Enum<E extends Enum<E>> implements Comparable<E> 
```

Example 2-3. Base class for enumerated types   
public abstract class Enum $<  \mathsf{E}$ extends Enum $<  \mathsf{E} > >$ implements Comparable $<  \mathsf{E}>$ { private final String name; private final int ordinal; protected Enum(String name, int ordinal) { this.name $=$ name; this.ordinal $=$ ordinal; } public final String name() { return name; } public final int ordinal() { return ordinal; } public String toString() { return name; } public final int compareTo(E o) { return ordinal - ((Enum<?>）o).ordinal; }

Example 2-4. Class corresponding to an enumerated type   
/\* corresponds to enum Season { WINTER, SPRING, SUMMER, FALL }   
\*/   
final class Season extends Enum<Season> { private Season(String name,int ordinal){ super(name,ordinal);} public static final Season WINTER $=$ new Season("WINTER",0); public static final Season SPRING $=$ new Season("SPRING",1); public static final Season SUMMER $=$ new Season("SUMMER",2); public static final Season FALL $\equiv$ new Season("FALL",3); private static final Season[] VALUES $=$ { WINTER, SPRING, SUMMER, FALL }; public static Season[] values(){ return VALUES.clone();} public static Season valueOf(String name){ for (Season e:VALUES) if (e.name().equals(name)) return e; throw new IllegalArgumentException(); 1   
1

Recursively bounded types like T extends Comparable<T> and E extends Enum<E> occur when we want to constrain a parameter to range only over the subtypes of a particular type. For example, in any subtype T of the base Enum class, the compareTo method can only be applied to an argument of type T, not to any other subtype of Enum: the definition provides a way of naming the type itself in the body of the class. (The term “self-type” is sometimes used for this in discussions of recursively bounded types.)

The rest of the definitions are straightforward. The base class Enum defines two fields, a string name and an integer ordinal, that are possessed by every instance of an enumerated type; the fields are final because once they are initialized, their value never changes. The constructor for the class is protected, to ensure that it is used only within subclasses of this class. Each enumeration class makes the constructor private, to ensure that it is used only to create the enumerated constants. For instance, the Season class has a private constructor that is invoked exactly four times in order to initialize the final variables WINTER, SPRING, SUMMER, and FALL.

The base class defines accessor methods for the name and ordinal fields. The toString method returns the name, and the compareTo method just returns the difference of the ordinals for the two enumerated values. (Unlike the definition of Event in “Comparable $\mathbf { \bar { \Phi } } _ { } < \mathbf { T } > ^ { \mathbf { \bar { \Phi } } _ { } }$ , this is safe because, since the ordinals are always positive, there is no possibility of overflow.) Hence, constants have the same ordering as their ordinals—for example, WINTER precedes SUMMER.

Lastly, there are two static methods in every class that corresponds to an enumerated type. The values method returns an array of all the constants of the type. It returns a (shallow) clone of the internal array. Cloning is vital to ensure that the client cannot alter the internal array. Note that you don’t need a cast when calling the clone method, because cloning for arrays now takes advantage of covariant return types (see “Covariant Overriding”). The valueOf method takes a string and returns the corresponding constant, found by searching the internal array. It returns an

IllegalArgumentException if the string does not name a value of the enumeration.

# Multiple Bounds

We have seen many examples where a type variable or wildcard is bounded by a single class or interface. In rare situations, it may be desirable to have multiple bounds, and we show how to do so here.

To demonstrate, we use three interfaces from the Java library. The Readable interface has a read method to read into a buffer from a character source, the Appendable interface has an append method to copy from a buffer into a target capable of receiving characters, and the Closeable interface has a close method to close a source or target. Possible sources and targets include character files, buffers, streams, and so on.

For maximum flexibility, we might want to write a copy method that accepts a Supplier of any source that implements both Readable and Closeable, and a Supplier of any target that implements both Appendable and Closeable. The method will copy the contents of the source to the target. Unfortunately, character readers and writers typically cannot be opened without potentially throwing IOException, so we need a specialized supplier interface:

```typescript
interface IoeThrowingSupplier<S> { S get() throws IOException; } 
```

The method copy can now be declared:

```java
public static <S extends Readable & Closeable, T extends Appendable & Closeable> void copy(IoehThrowingSupplier<S> src, IoeThrowingSupplier<T> tgt, int size) throws IOException { 
```

```txt
try (S s = src.get(); T t = tgt.get()) { CharBuffer buf = CharBuffer.allocate(size); int i = s.read(buf); while (i >= 0) { bufflip(); // prepare buffer for writing t.append(buf); buf.clear(); // prepare buffer for reading i = s.read(buf); } } } 
```

The first line of this method specifies that S ranges over any type that implements both Readable and Closeable, and that T ranges over any type that implements Appendable and Closeable. When multiple bounds on a type variable appear, they are separated by ampersands.

The try-with-resources statement evaluates the supplied lambdas, which open and return a character source and a character target. It then repeatedly reads from the source into a buffer and appends from the buffer into a target. When the source is empty, the try block is exited, closing both the source and the target. As an example, this method may be called with a FileReader and a FileWriter as source and target:

```txt
int size = 32;  
Files.writeString(Path.of("file.in"), "hello world");  
copy((   ) -> new FileReader("file.in"),  
(   ) -> new FileWriter("file.out"), size);  
assert Files.readString(Path.of("File.out").equals("hello world")); 
```

Other possible sources include FilterReader, PipedReader, and StringReader, and other possible targets include FilterWriter, PipedWriter, and PrintStream. But you could not use StringBuffer as a target, since it implements Appendable but not Closeable.

If you are familiar with the java.io library, you may have spotted that all classes that implement both Readable and Closeable are subclasses of Reader, and almost all classes that implement Appendable and

Closeable are subclasses of Writer. So you might wonder why we don’t simplify the method signature like this:

public static void copy(Reader src, Writer trg, int size)

This will indeed admit most of the same classes, but not all of them. For instance, PrintStream implements Appendable and Closeable but is not a subclass of Writer. Furthermore, you can’t rule out the possibility that some programmer using your code might have his or her own custom class that, say, implements Readable and Closeable but is not a subclass of Reader.

When multiple bounds appear, the first bound is used for erasure. We saw this used earlier in “Maximum of a Collection”:

public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll)

Without the highlighted text, the erased type signature for max would have Comparable as the return type, whereas in legacy libraries the return type is Object. Maintaining compatibility with legacy libraries is further discussed in [Link to Come] and [Link to Come].

# Bridges

As we mentioned earlier, generics are implemented by erasure: the compiled representation of code written with generics is almost exactly the same as that written without. However, in the case of a parameterized supertype—for example, an interface such as Comparable<T>—erasure may require additional methods to be inserted by the compiler; these additional methods are called bridges.

Example 2-5 shows the Comparable interface and a simple Point record implementing that interface. The natural order on Points is defined by comparing their distances from the origin (or, more precisely, the squares

of their distances from the origin: it amounts to the same thing). The declaration of compareTo in Point overrides that in Comparable, because the signatures match.

Example 2-5. Generic code implementing a parameterized interface   
```typescript
interface Comparable<T> { public int compareTo(T other); record Point(double x, double y) implements Comparable<Point> { public int compareTo(Point p) { return Double compareTo(this.x * this.x + this.y * this.y, p.x * p.x + p.y * p.y); } } 
```

Erasure changes the situation, however, as shown in Example 2-6.

Example 2-6. Parameterized interface and implementation after erasure   
```javascript
interface Comparable { public int compareTo(Object other); record Point(double x, double y) implements Comparable { // fails compilation public int compareTo(Double p) { return Double compareTo(this.x * this.x + this.y * this.y, p.x * p.x + p.y * p.y); } } 
```

The signature of compareTo in the interface has changed, and to restore overriding the compiler must add an extra method to the implementation:

```txt
public int compareTo(Object other) { return compareTo((Point)other); } 
```

You can confirm the existence of this bridge using reflection:

Map<class $?，Boolean> typeToBridge $=$ (Arrays.stream	Point.class.getMethods()) .filter(m -> m.getName().equals("compareTo"))

```cpp
. collect (Collectors.toMap(m -> m.getParameterTypes() [0], Method::isBridge)); assert typeToBridge.size() == 2; assert ! typeToBridge.get(Class); // compareTo Point) is not a bridge method assert typeToBridge.get(Object.class); // compareTo(Object) is a bridge method 
```

And the entire declaration of the method can be seen by decompiling the class file, for example with the open source decompiler FernFlower, which produces this output:

```txt
// $FF: synthetic method
// $FF: bridge method
public int compareTo(Object var1) {
    return this compareTo((Point)var1);
} 
```

Bridge methods are required whenever a class or interface implements or extends a parameterised supertype by instantiating its type parameter3 .

Bridges can play an important role when converting legacy code to use generics; see [Link to Come].

# Covariant Overriding

At the same time that generics were introduced, Java started supporting covariant method overriding. This feature is not directly related to generics, but we discuss it here because it is worth knowing, and because it is implemented using a bridging technique like that described in the previous section.

In early versions of Java, one method could override another only if the signatures and the return types matched exactly. With covariant overriding, the signatures must still match (after erasure), but the requirement to match the return types is relaxed so that now the return type of the overriding method need only be a subtype of the return type of the overridden method.

The clone method of class Object illustrates the advantages of covariant overriding:

```txt
class Object { protected Object clone() { } 
```

Without covariant overriding, any class that overrides clone has to give the overriding method exactly the same return type, namely Object. For example, here is a Point record that does this:

```txt
record Point(double x, double y) { public Object clone() { return new Point(x,y); } 
```

Here, even though clone always returns a Point, without covariance the old rules required it to have the return type Object. This was annoying, since every invocation of clone had to cast its result.

```txt
Point p = new Point(1,2);  
Point q = (Point)p.clone(); 
```

With covariant overriding, it is possible to give the clone method a return type that is more to the point, as it were:

```txt
record Point(double x, double y) { public Point clone() { return new Point(x,y); } 
```

Now we may clone without a cast:

```txt
Point p = new Point(1,2);  
Point q = p.clone(); 
```

Covariant overriding is implemented using the bridging technique described in the previous section. As before, you can see the bridge using decompilation or by applying reflection. Here is code that finds all methods

with the name clone in the class Point, then maps the return type of each overload to the result of calling the Method method isBridge:

```cpp
Map<class $?Boolean> returnToBridge = (Arrays.stream(Class.class-Methods())
                     .filter(m -> m.getName().equals("clone"))
                     .collect(Collectors.toMap(Method::getReturnType,
                                     Method::isBridge));
assert returnToBridge.size() == 2;
assert returnToBridge.get(Object.class); // Object
clone(Point) is a bridge method
assert ! returnToBridge.get(Object.class); // Point
clone(Point) is not a bridge method 
```

Here the bridging technique exploits the fact that in a class file two methods of the same class may have the same argument signature, even though this is not permitted in Java source. The bridge method simply calls the first method.

1 See https://www.lambdafaq.org/what-is-a-functional-interface/   
2 The code for Example 2-3 does not correspond on all points with the source in the Java library, and most of the code corresponding to Example 2-4 is actually synthesized by the Java compiler, but these examples are functionally equivalent to the code that is actually executed.   
3 Strictly speaking, this is true only if erasure changes the signature of any of the supertype‘s methods.

OceanofPDF.com

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 4th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

This chapter discusses how to declare a generic class. It describes constructors, static members, and nested classes, and it fills in some details of how erasure works.

# Constructors

In a generic class, type parameters appear in the header that declares the class, but not in the constructor:

```txt
class Pair<T,U> {
    private final T first;
    private final U second;
    public Pair(T first, U second) {
        this.first = first;
        this(second = second);
    }
    public T getFirst() { return first; }
    public U getSecond() { return second; }
} 
```

The type parameters T and U are declared at the beginning of the class, not in the constructor. However, actual type parameters are passed to the constructor whenever it is invoked:

Pair<String, Integer> pair1 $=$ new Pair<String, Integer>("one",2); assert pair1.getFirst().equals("one") && pair1.getSecond() $\ c = ~ 2$ ;

Look Out for This! A common mistake is to forget the type parameters when invoking the constructor:

Pair<String, Integer> pair2 $=$ new Pair("one",2);

This mistake produces a warning, but not an error. It is taken to be legal, because Pair is treated as a raw type, a type containing no parametric type information but which can be converted to the corresponding parameterized type, generating only an unchecked warning (see [Link to Come], which explains raw types and how the -Xlint:unchecked flag can help you spot errors of this kind).

Records can be parameterised in just the same way as regular classes:

record Pair<T, $\mathsf { U } >$ (T first, U second) {}

# Static Members

Compilation by erasure means that at run time parameterised types are replaced by the corresponding raw type: for example, the interfaces List<Integer>, List<String>, and List<List<String>> are all implemented by a single interface, namely List:

List<Integer> ints $=$ Arrays.asList(1,2,3); List<String> strings $=$ Arrays.asList("one","two"); assert ints.getClass() $= =$ strings.getClass();

We see here that the class object associated with a list of integers at run time is the same as the one associated with a list of strings.

One consequence is that static members of a generic class are shared across all instantiations of that class, including instantiations at different types. Static members of a class cannot refer to the type parameter of a generic class, and when accessing a static member the class name should not be parameterized.

For example, here is a class, Cell<T>, in which each cell has an integer identifier and a value of type T:

```java
class Cell<T> {
    private final int id;
    private final T value;
    private final static AtomicInteger count = new AtomicInteger();
    private static int nextId() { return count.getAndIncrement();
} 
public Cell(T value) {
    this.value = value;
    id = nextId();
} 
public T getValue() { return value; }
public int getId() { return id; }
public static int getCount() { return count.get(); } 
```

A static field, count, is used to allocate a distinct identifier to each cell. For the count, an AtomicInteger is used to ensure that unique identifiers are generated even under concurrent access. The static getCount method returns the current count.

Here is code that allocates a cell containing a string and a cell containing an integer, which are allocated the identifiers 0 and 1, respectively:

```typescript
Cell<String> a = new Cell<String>()("one");
Cell<Integer> b = new Cellanches>(2);
assert a.getId() == 0 && b.getId() == 1 && Cell-count() == 2; 
```

Static members are shared across all instantiations of a class, so the same count is incremented when allocating either a string or an integer cell.

Because static members are independent of any type parameters, we are not permitted to follow the class name with type parameters when accessing a static member:

```txt
Cell.getCount(); //ok  
Cell<Integer>.getCount(); //compile-time error  
Cell<?>.getCount(); //compile-time error 
```

The count is static, so it is a property of the class as a whole, not any particular instance.

For the same reason, you can’t refer to a type parameter anywhere in the declaration of a static member. Here is a second version of Cell, which attempts to use a static variable to keep a list of all values stored in any cell:

```java
class Cell2<T> {
    private final T value;
    private static List<T> values = new ArrayList<T>(); // illegal
    public Cell2(T value) {
        this.value=value;
        values.add(value);
    }
    public T getValue() { return value; }
    public static List<T> VALUES() { return values; } // illegal 
```

Since the class may be used with different type parameters at different places, it makes no sense to refer to T in the declaration of the static field values or the static method getValues, and these lines are reported as errors at compile time. If we want a list of all values kept in cells, then we need to use a list of objects, as in the following variant:

```java
class Cell2<T> {
    private final T value;
    private static List<object> values = new ArrayList<object>(); // ok
    public Cell2(T value) {
        this.value=value;
        values.add(value);
    }
    public T getValue() { return value; } 
```

```java
public static List<object> VALUES() { return values; } // ok 
```

This code compiles and runs with no difficulty:

```txt
Cell12<String> a = new Cell12<String>()("one");
Cell12Integer> b = new Cell12Integer>(2);
assert Cell12.Values().equals(List.of("one", 2)); 
```

# Nested Classes

Java permits nesting one class inside another. If the outer class has type parameters and the inner class is a member class—that is, not static—then type parameters of the outer class are visible within the inner class.

Example 3-1 shows a class implementing collections as a singly-linked list. This class extends java.util.AbstractCollection, so it only needs to define the methods size, add, and iterator (see [Link to Come]). It contains an inner class, Node, for the list nodes, and an anonymous inner class implementing Iterator<E>. The type parameter E is in scope within both of these classes.

Example 3-1. Type parameters are in scope for member classes   
```java
class LinkedCollection<E> extends AbstractCollection<E> {
    private class Node {
        private E element;
        private Node next = null;
        private Node(E elt) { element = elt; }
    }
    private Node first = new Node(null);
    private Node last = first;
    private int size = 0;
    public LinkedCollection() {}
    public LinkedCollection(Collection<? extends E> c) { addAll(c); }
    public int size() { return size; }
    public boolean add(E elt) {
        last.next = new Node(elt); last = last.next; size++;
        return true;
    }
    public Iterator<E> iterator() {
        return new Iterator<E>() { 
```

private Node current $=$ first;   
public boolean hasNext() { return current.next $! =$ null;   
public E next(){ if(current.next $! =$ null){ current $=$ current.next; return current.element; } else throw new NoSuchElementException(); public void remove(){ throw newUnsupportedOperationException(); } };

For contrast, Example 3-2 shows a similar implementation, but this time the inner Node class is static, and so the type parameter E is not in scope for this class. Instead, the inner class is declared with its own type parameter, T. Where the previous version referred to Node, the new version refers to Node<E>. The anonymous iterator class in the preceding example has also been replaced by a static inner class, again with its own type parameter.

If the node classes had been made public rather than private, you would refer to the node class in the first example as

LinkedCollection<E>.Node, whereas you would refer to the node class in the second example as LinkedCollection.Node<E>.

Example 3-2. Type parameters are not in scope for static inner classes   
```java
class LinkedCollection<T> extends AbstractCollection<E> {
    private static class Node<T> {
        private T element;
        private Node<T> next = null;
        private Node(T elt) { element = elt; }
    }
    private Node<T> first = new Node<T>(null);
    private Node<T> last = first;
    private int size = 0;
    public LinkedCollection() {}
    public LinkedCollection(Collection<? extends E> c) { addAll(c); }
    public int size() { return size; }
    public boolean add(E elt) {
        last.next = new Node<T>(elt); last = last.next; size++;
    }
} 
```

```java
return true;   
}   
private static class LinkedIterator<T> implements Iterator<T> { private Node<T> current; public LinkedIterator(Node<T> first) { current = first; } public boolean hasNext() { return current.next != null; } public T next(){ if(current.next != null){ current = current.next; return current.element; } else throw new NoSuchElementException(); } public void remove(){ throw new UnsupportedOperationException(); }   
public Iterator<E> iterator(){ return new LinkedIterator<E>(first);   
} 
```

Of the two alternatives described here, the second is preferable. Member classes are implemented by including a reference to the enclosing instance, since they may, in general, access components of that instance. Static inner classes are usually both simpler and more efficient.

# How Erasure Works

The erasure of a type is defined as follows: drop all type parameters from parameterized types, and replace any type variable with the erasure of its bound, or with Object if it has no bound, or with the erasure of the leftmost bound if it has multiple bounds. Here are some examples:

The erasure of List<Integer>, List<String>, and List<List<String>> is List.   
The erasure of List<Integer>[] is List[].   
The erasure of List is itself, similarly for any raw type .

The erasure of int is itself, similarly for any primitive type.   
The erasure of Integer is itself, similarly for any type without type parameters.   
The erasure of T in the definition of toList (see [Link to Come]) is Object, because T has no bound.   
The erasure of T in the definition of max (see “Maximum of a Collection”) is Comparable, because T has bound Comparable<? super T>.   
The erasure of T in the final definition of max (see “Multiple Bounds”) is Object, because T has bound Object & Comparable<T> and we take the erasure of the leftmost bound.   
The erasures of S and T in the definition of copy (see “Multiple Bounds”) are Readable and Appendable, because S has bound Readable & Closeable and T has bound Appendable & Closeable.   
The erasure of LinkedCollection<E>.Node or LinkedCollection.Node<E> (see “Nested Classes”) is LinkedCollection.Node.

In Java, two methods of the same class cannot have the same signature— that is, the same name and parameter types. Since generics are implemented by erasure, it also follows that two distinct methods cannot have signatures with the same erasure. A class cannot overload two methods whose signatures have the same erasure, and a class cannot implement two interfaces that have the same erasure.

For example, here is a class with two convenience methods. One adds together every integer in a list of integers, and the other concatenates together every string in a list of strings:

```java
class Overloaded { public static int sum(List<Integer> ints) { 
```

int sum $= 0$ for (int i : ints) sum += i; return sum;   
public static String sum(List<String> strings) { StringBuffer sum $=$ new StringBuffer(); for (String s : strings) sum.append(s); return sum.toString();   
}

Here are the erasures of the declarations of the two methods:

```txt
int sum(List)  
String sum(List) 
```

But it is the signatures alone, not the return types, that allow the Java compiler to distinguish different method overloads. In this case the erasures of the signatures of both methods are identical:

```txt
sum(List) 
```

So a name clash is reported at compile time.

For another example, here is a bad version of the Integer class, that tries to make it possible to compare an integer with either an integer or a long:

```javascript
class Integer implements Comparable<Integer>, Comparable<Long> { // compile-time error, cannot implement two interfaces with same erasure private final int value; public int compareTo(Integer i) { return (value < i.value) ? -1 : (value == i.value) ? 0 : 1; } public int compareTo(Long 1) { return (value < 1.intValue()) ? -1 : (value == 1.intValue()) ? 0 : 1; } } 
```

If this were supported, it would, in general, require a complex and confusing definition of bridge methods (see “Bridges”). The simplest and most understandable option by far is to ban this case.

OceanofPDF.com

# Chapter 4. The Main Interfaces of the Java Collections Framework

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 10th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

A collection is an object that provides access to a group of objects, allowing them to be processed in a uniform way. A collections framework provides a uniform view of a set of collection types specifying and implementing common data structures, following consistent design rules so that they can work together. Figure 4-1 shows the main interfaces of the Java Collections Framework, together with one other—Iterable—which is outside the Framework. Iterable (see “Iterable and Iterators”) defines the contract that a class—any class, not only one of those in the Framework—must implement in order to be used as the target for an “enhanced for statement”, usually called a foreach statement.

![](images/b29c56c8008864c838cce5dbdff63fa690a05149cc00305605751a8a0ef845ab.jpg)  
Figure 4-1. The main interfaces of the Java Collections Framework

The Framework interfaces in Figure 4-1 have the following purposes:

# Collection

Collection exposes the core functionality required of any collection other than a Map. Its methods support managing elements by adding or removing single or multiple elements, checking membership of a single or multiple values, and inspecting and exporting elements. It has no direct concrete implementations; the concrete collection classes all implement one of its subinterfaces as well.

# Set

A Set is a collection in which order is not significant and contains no duplicates. It adds no operations to Collection, but the contracts for its element-adding operations specify that they will not create duplicates.

# List

A List is a collection in which order is significant and which accommodates duplicate elements. It adds operations supporting indexed access.

# Queue

A Queue is a collection whose additional operations accept elements at one end, its tail, for processing, and yield them up at the other end, its head, in

the order in which they are to be processed.

# Map

A Map is a collection which uses key-value associations to store and retrieve elements. The keys of a Map form a Set, so they follow the rule that there is no duplication and ordering is not significant. The operations of Map allow it to be maintained by addition and removal of single or multiple key-value associations, but these are ancillary to the main use of Maps, which is to find the values that correspond to given keys.

# Using the Different Collection Types

A simple example, a word cloud generator, will illustrate the purpose of some of these types. Word cloud generators normally ignore the order of the words in their input, so any of the word clouds in Figure 4-2 could be produced from the Set created by

Set.of("Larry", "Curly", "Moe")

![](images/53d167f8875084545fd72977fd45df9ce8c9f640dbac58fe16ed5bf75233fbce.jpg)  
Figure 4-2. Word clouds generated from Set data

Although the position of words in a cloud is randomly determined, their size depends on the number of repetitions of each in the input to the generator. So, since Sets cannot store duplicate elements, they are not actually a suitable data structure for representing the contents of a word cloud. For a collection type that does accommodates duplicates, we must turn to List. The word cloud generated by

List.of("Larry", "Larry", Curly", "Moe")

will be like one of those in Figure 4-3, reflecting the fact that the string "Larry" occurs twice as often as the other two. (Again, the random order of the words in Figure 4-3 is not because the order has been lost from the collection but because word cloud generators ignore the order of their input.)

![](images/ec691c3f8003bcd556e57835000edf5e6c601b22828e3d37eddb0792416bdd54.jpg)

![](images/88fcb25160ac6ac7e83a5f1d5dbb12c61c293220cdb6a128bc11adeaf0139fa7.jpg)

![](images/d734e5ff83c722666e67ff9dabd4303e41510aefe927a5a82008549137e41011.jpg)  
Figure 4-3. Word clouds generated from List data

Since any Set can be represented by a List--one without duplicates and whose order is ignored—why have a separate data type at all? The reason is that the operations of Set make it much easier to guarantee element uniqueness, when that is what we actually want, and being able to disregard order can allow operations to be much more efficient.

In our word cloud example, a better representation of the input list would be a Map associating each word with its frequency:

Map.of("Larry", 2, "Curly", 1, "Moe", 1)

You can think of a Map as a table, one of those shown in Figure 4-4:

![](images/8b26ccaa3eb05fbdbc5eb86ab40439d39c13921101f6781dab4079f15cb6d897.jpg)

![](images/afbd59ff7f6cd79d2831443f0f53e4f0975321e342fc9142604b6348af2c7779.jpg)  
Figure 4-4. Word cloud data represented as a Map

KeyValue

“Moe” 1

“Larry” 2

“Curly” 1

The three tables in Figure 4-4 represent the same Map, because the table rows—that is, the key-value pairs that make up the Map--form a set, so that their order is not significant. Further, the keys also form a set, so there couldn’t be two rows with the same key, “Larry” say.

# Sequenced Collections

In the word cloud example, the order of the elements wasn’t important. Often, though, the ordering of elements is significant, and many collections, for example List, do preserve it—and, sometimes, impose it. Such collections are said to be sequenced. A sequenced collection is one with a defined order—unlike Collection, Set or Map--that can also be iterated in either direction, unlike Queue. Sequenced collections differ in how their ordering is derived: for some, like List, elements retain the order in which they were added, whereas for others, like NavigableSet (see below), the ordering is dictated by the values of the elements. These are sometimes called externally ordered and internally ordered types, respectively, reflecting the difference between an order which is arbitrarily imposed on the elements, for example by the order in which they are added,

and an order that is an inherent property of the elements themselves, for example by alphabetic ordering on strings.

A new interface, SequencedCollection, was introduced in Java ?? to unify the sequenced collections, of both kinds. Figure 4-5 adds the sequenced interfaces of the Framework to those of Figure 4-1.

![](images/ee0742c634d9f83ad88891e62ac191244e4a85be37c1233fa788ebe4e00f8f8b.jpg)

![](images/7502c3358259899e4286060ccddabdd78ef844636873fd5b6628d17eaed2be2c.jpg)  
Figure 4-5. The sequenced interfaces of the Java Collections Framework

# SequencedCollection

SequencedCollection provides a reversed view (see §11.6), together with the operations that all sequenced collections must support on the first and last elements of the collection: access, addition, and removal.

# SequencedSet and NavigableSet

A SequencedSet is an externally or internally ordered Set that also exposes the methods of SequencedCollection. A NavigableSet is an internally ordered SequencedSet that therefore also automatically

sorts its elements, and also provides additional methods to find elements adjacent to a target value.

# Deque

A Deque is a double-ended queue that can both accept and yield up elements at either end.

# SequencedMap and NavigableMap

A SequencedMap is a Map whose keys form a SequencedSet. A NavigableMap is a SequencedMap whose keys form a NavigableSet, so that its entries are automatically sorted by the key ordering, and its methods can find keys and key-value pairs adjacent to a target key value.

Chapters Chapter 6 through Chapter 11 will concentrate on each of the Collections Framework interfaces in turn. First, though, in Chapter 5, we need to cover some preliminary ideas which run through the entire Framework design.

OceanofPDF.com

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 11th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

In this chapter, we will take time to discuss the concepts underlying the framework, before we get into the detail of the collections themselves.

# Iterable and Iterators

An iterator is an object that implements the interface Iterator:

```txt
public Iterator<E> { boolean hasNext(); // return true if the iteration has further elements E next(); // return the next element in the iteration void remove(); // remove the last element returned by the iterator } 
```

The purpose of iterators is to provide a uniform way of accessing collection elements sequentially, so whatever kind of collection you are dealing with, and however it is implemented, you always know how to process its

elements in turn. Iterators are often not used directly, because in many situations the foreach statement is more convenient: compare the code to print every element of a collection coll using foreach:

```txt
for (Object o : coll) { System.out.println(o); } 
```

with code explicitly using an iterator:

```txt
for (Iterator itr = coll.iterator(); itr.hasNext(); ) { System.out.println(itr.next()); } 
```

The target of a foreach statement can be an array, or any class that implements the interface Iterable:

```swift
public Iterator<T> {  
    Iterator<T> iterator(); // return an iterator over elements of type T  
} 
```

The Collection interface extends Iterable, so any set, list, or queue can be the target of foreach. If you write your own implementation of Iterable, that too can be used with foreach. ??? shows an example. A Counter object is initialized with a count of Integer objects; its iterator returns these in ascending order in response to calls of next. Now Counter objects can be the target of a foreach statement:

int total $= 0$ for (int i : new Counter(3)) { total $+ = \mathrm{i}$ }   
assert total $= = 6$

In practice, it is unusual to implement Iterable directly in this way, as foreach is most commonly used with arrays and the standard collections classes. Direct use of Iterators, rather than a foreach statement, is

necessary mainly when you want to make a structural change to a collection—broadly speaking, adding or removing elements—in the course of iteration. As we just saw, the Iterator interface exposes only a method for removal of collection elements, though its subinterface ListIterator, available to List implementations, also provides methods to add and replace elements.

At one time or another, you may have made the mistake of trying, during iteration, to make a structural change directly—that is, rather than using one of the iterator methods. If the collection that you were iterating over was one of the general-purpose Collection implementations —

ArrayList, HashMap, and so on—you may have been puzzled by seeing ConcurrentModificationException thrown from singlethreaded code. The iterators of these collections throw this exception whenever they detect that the collection from which they were derived has been structurally modified—except by the iterator itself, of course.

The motivation for this behavior is that a structural change made during iteration is possibly evidence that another thread is accessing the collection; structural changes that you wish the iterating thread to make should be made using the iterator. Allowing another thread to access a non-thread-safe collection will probably result in a failure some time later, when it will be difficult to diagnose. To avoid this problem the general-purpose Collections Framework iterators are fail-fast: their methods check for any structural modifications made since the last iterator method call, throwing ConcurrentModificationException if they detect one. Although this restriction rules out some sound programs, it rules out many more unsound ones.

```java
class Counter implements Iterator<String> {
    private int count;
    public Counter(int count) { this.count = count; }
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private int i = 0;
            public boolean hasNext() { return i < count; }
            public Integer next() { i++; return i; }
            public void remove(){
                throw new
            }
        }
    }
} 
```

```txt
UnsupportedOperationException();} } } 
```

Now Counter objects can be the target of a foreach statement:

int total $= 0$ for (int i : new Counter(3)) { total $+ = \mathrm{i}$ }   
assert total $= = 6$

In practice, it is unusual to implement Iterable directly in this way, as foreach is most commonly used with arrays and the standard collections classes. Direct use of Iterators, rather than a foreach statement, is necessary mainly when you want to make a structural change to a collection—broadly speaking, adding or removing elements—in the course of iteration. As we just saw, the Iterator interface exposes only a method for removal of collection elements, though its subinterface ListIterator, available to List implementations, also provides methods to add and replace elements.

At one time or another, you may have made the mistake of trying, during iteration, to make a structural change directly—that is, rather than using one of the iterator methods. If the collection that you were iterating over was one of the general-purpose Collection implementations —

ArrayList, HashMap, and so on—you may have been puzzled by seeing ConcurrentModificationException thrown from singlethreaded code. The iterators of these collections throw this exception whenever they detect that the collection from which they were derived has been structurally modified—except by the iterator itself, of course.

The motivation for this behavior is that a structural change made during iteration is possibly evidence that another thread is accessing the collection; structural changes that you wish the iterating thread to make should be made using the iterator. Allowing another thread to access a non-thread-safe

collection will probably result in a failure some time later, when it will be difficult to diagnose. To avoid this problem the general-purpose Collections Framework iterators are fail-fast: their methods check for any structural modifications made since the last iterator method call, throwing ConcurrentModificationException if they detect one. Although this restriction rules out some sound programs, it rules out many more unsound ones.

The concurrent collections have other strategies for handling concurrent modification, such as weakly consistent iterators. We discuss them in more detail in “Collections and Thread Safety”.

# Implementations

We have looked briefly at the interfaces of the Collections Framework, which define the behavior that we can expect of each collection. But as we mentioned in the introduction to this chapter, there are several ways of implementing each of these interfaces. Why doesn’t the Framework just use the best implementation for each interface? That would certainly make life simpler—too simple, in fact, to be anything like life really is. If an implementation is a greyhound for some operations, Murphy’s Law tells us that it will be a tortoise for others. Because there is no “best” implementation of any of the interfaces, you have to make a tradeoff, judging which operations are used most frequently in your application and choosing the implementation that optimizes those operations.

The three main kinds of operations that most collection interfaces require are insertion and removal of elements by position, retrieval of elements by content, and iteration over the collection elements. The implementations provide many variations on these operations, but the main differences among them can be discussed in terms of how they carry out these three. In this section, we’ll briefly survey the four main structures used as the basis of the implementations and later, as we need them, we will look at each in more detail. The four structures are:

# Arrays

These are the structures familiar from the Java language—and just about every other programming language since Fortran. Because arrays are implemented directly in hardware, they have the properties of random-access memory: very fast for accessing elements by position and for iterating over them, but slower for inserting and removing elements at arbitrary positions (because that may require adjusting the position of other elements). Arrays are used in the Collections Framework as the backing structure for ArrayList,

CopyOnWriteArrayList, EnumSet and EnumMap, and for many of the Queue and Deque implementations. They also form an important part of the mechanism for implementing hash tables (discussed shortly).

# Linear linked lists

As the name implies, these consist of chains of linked cells. Each cell contains a reference to data and a reference to the next cell in the list (and, in some implementations, the previous cell). Linked lists perform quite differently from arrays: accessing elements by position is slow, because you have to follow the reference chain from the start of the list, but insertion and removal operations can be performed in constant time by rearranging the cell references. Linked lists are the primary backing structure used for the classes ConcurrentLinkedQueue, LinkedBlockingQueue, and LinkedList.

# Other linked data structures

Linked structures are particularly suitable for representing nonlinear types like trees and skip lists, especially if they need to be rearranged as new elements are added. Such structures provide an inexpensive way of maintaining sorted order in their data, allowing fast searching by content. Trees are the backing structures for TreeSet and TreeMap. Skip lists are used in SkipListSet and SkipListMap. Priority

heaps, used in the implementation of PriorityQueue and PriorityBlockingQueue, are tree-related structures.

# Hash tables

These provide a way of storing elements indexed on their content rather than on an integer-valued index, as with lists. In contrast to arrays and linked lists, hash tables provide no support for accessing elements by position, but access by content is usally very fast, as are insertion and removal. Hash tables are the backing structure for many Set and Map implementations, including HashSet and LinkedHashSet together with the corresponding maps HashMap and LinkedHashMap, as well as WeakHashMap, IdentityHashMap and ConcurrentHashMap.

The content-based indexing of hashed collections depends on two Object methods, hashCode and equals, which are applied in succession to position an element for insertion or to locate it for retrieval. So it is in the contracts for these methods, specifically for Object.hashCode, that we find the requirements that hashed collections place on their relationship. The crucial requirement, sometimes overlooked—with disastrous consequences—by Java programmers, is that if two objects are equal according to the equals method, then calling hashCode on each must produce the same result. If hashCode depends on an instance field—or any other data about an object—that is not used by the equals method, then the first stage of a retrieval operation will very likely be misdirected. One way in which this can occur is if you do not override

Object.hashCode at all; the value that it returns in this case will be implementation-dependent (in OpenJDK it is usually randomly generated), but is in any case highly unlikely to be the same for two different instances.

The toy class Person is a case in point:

```java
class Person {
    private String name;
    public Person(String name) { 
```

this.name $=$ name;   
publicbooleanequals(Objecto){ return o instanceof Person p &&name.equals(p.name);   
}

This class should override hashCode so that it depends only on the same field, i.e. name, that equals depends on. But it does not; as a result, hashed collections will not work correctly with it:

```txt
Set<Person> people = newHashSet<>();  
people.add(new Person("Alice"));  
assert! people.contains(new Person("Alice")); 
```

# Views

In the Collections Framework, a view of a data structure provides a way of working with it as though it had been transformed in some way—either into a differently-organised structure of the same type, or a structure of another type completely. The most obvious examples of this, in a general sense, are the backing structures discussed in the last section. But even though replacing an element in an ArrayList, for example, is implemented by replacing an element in the underlying array, we don’t normally refer to an ArrayList as a view of an array. That’s because ArrayList is more than just a view; it’s capable of insulating its user from the limitations of arrays: for example, you frequently want to add elements to a list. Arrays can’t support that, but ArrayList hides that limitation, if necessary by transferring all its elements to a new and larger backing array. So an ArrayList is much more than a simple view of a single array, and that term isn’t usually used to describe it.

Sometimes, however, a simple view is what is needed. For example, suppose we want to test for the presence of a particular object in an array. One obvious way to do this is to iterate over the array elements, testing each for equality with the search target. An alternative would be to use the contains method of the List interface to do that work instead. An array

is not a List, though, so how can a List method be useful in handling it? We might well want to avoid the overhead of creating a new ArrayList object, physically copying all the elements of the array into a new collection. In this situation, a better answer is to get a List view of the array—an object that “looks like” a List, but implements all its operations directly on the underlying array. The method asList of the utility class Arrays provides such a view. The simple view that it returns supports some List operations, such as contains, and methods like get and set which access or replace the array elements, but it won’t allow you to make structural modifications, like adding or removing elements, which aren’t supported by the underlying array.

The data “of” the view actually resides in the underlying structure, so changes made to that structure are immediately visible in the view, and vice versa. For example, the following code compiles and runs without errors:

```txt
Integer[] arr = {1,2,3};  
var list = Arrays.asList(arr);  
list.set(0,3);  
assert arr[0] == 3;  
arr[2] = 0;  
assert list.get(2) == 0; 
```

The Collections API exposes many methods returning views—for example, the keys of a Map can be viewed as a Set, as can its entries; collections can be viewed as unmodifiable, and so on. Each of these views has different rules dictating which modifications they will allow: some allow certain structural modifications, some allow only non-structural modifications, and some are fully unmodifiable. So the interfaces that these views implement have some of their operations labelled optional: this is perhaps the most controversial aspect of the Java Collections Framework design. We’ll discuss it further in the section “Contracts” and in Chapter [Link to Come]. The views themselves will be discussed in subsequent chapters, each one in the context of its backing collection.

# Performance

The usefulness of a collections library depends on the impact of the performance of its collections as part of a working system. Unfortunately, this is very difficult both to predict in theory and to assess in practice. Many factors contribute to it, including:

how often any of the collection’s operations are executed   
which operations are executed most frequently   
the time cost of each of the operations that are executed   
how much garbage each produces, and what overhead is incurred in collecting it   
the locality properties (see “Memory”) of the collection   
how much parallelism is involved, both at instruction and thread level

The study of how these factors combine to affect the speed of a real-life system belongs to the subject of performance tuning, of which the most important rule is often quoted in the form provided by Donald Knuth ([Knuth74]): “premature optimization is the root of all evil.” The explanation of this remark is twofold: firstly, for many programs, performance is just not a critical issue. If a program is rarely executed or already uses few resources, optimization is a waste of effort, and indeed may well be harmful. Secondly, even for performance-critical programs, assessing which part is critical normally requires accurate measurement: in the same paper, Knuth added “It is often a mistake to make a priori judgments about what parts of a program are really critical, since the universal experience of programmers who have been using measurement tools has been that their intuitive guesses fail.”

This carries an important implication about comparing the performance of different collections. For example, CopyOnWriteArrayList provides highly efficient concurrent read operations, at the cost of very expensive writes. So to use it in a system that requires highly performant concurrent

access to a List you have to have confidence, gained by measurement if necessary, that read operations greatly outnumber writes.

# Memory

Traditionally, an algorithm was assessed on the basis of its use of two resources: time and space. With the forty-year exponential decrease in the cost of memory from the 1970s onwards, space complexity became less important, and the focus sharpened on time complexity (see “Instruction Count and the O-notation”). But modern trends in both software and hardware systems architecture have complicated the picture by reintroducing memory concerns as a major focus. The software concern is relatively straightforward: for Java programs, memory temporarily allocated in the course of program execution must be reclaimed by the garbage collector, and garbage collection is an expensive overhead to the operation of any program.

The effects of modern trends in hardware design require more explanation. For four decades, from the 1970s onwards, processor speeds rose exponentially, far outstripping the speed increase of every other component. These include memory and the memory bus, the components that together are responsible for keeping processors supplied with data and instructions to work on. The different grades of memory available conform to the same rule that governs storage technologies of all kinds, including on-board memory, disk drives of all kinds, and even tape (still in use for archival storage): the cost of storage is inversely correlated with the speed of the medium. The faster the medium can store and retrieve data, the greater the cost per byte of capacity. This leads designers to create memory hierarchies, with large amounts of cheap storage, like disk and even tape, at the bottom, and small amounts of expensive static RAM located on-chip, physically close to the processor, at the top. Figure Figure 5-1 shows a highly simplified view of the top part—the on-board and on-chip components—of the hierarchy. On-chip memory is organised in caches called Level 1, Level 2, and (sometimes) Level 3, each one slower and larger (and less powerhungry) than its predecessor.

![](images/cf8636ab08a39a6afb0fe7619ddf85d47cc0eeddbe5b639181d5a3dabdfe0d2c.jpg)  
Figure 5-1. The memory hierarchy

If a data item is not available at one level, lower levels are searched until it is found. If the level is a cache, this is called a cache miss. Data in caches is organised in units called cache lines, commonly 64 bytes. If a failure to find one byte of data causes a cache miss, an entire cache line must be discarded in order to load that byte from a lower level in the hierarchy, along with the 63 bytes adjacent to it in memory. Such expensive cache misses will occur less often for programs that conform to the principle of spatial locality, which says that programs tend to reuse data and instructions near to those that they have used recently. Two programs, each of which execute the

same number of instructions, may have very different execution times if one exhibits spatial locality and the other does not.

Object-oriented programs often exhibit poor spatial locality, because objects can be located anywhere in memory, but some are worse than others. For example, a program iterating over an array of primitives will show good spatial locality, and also a predictable pattern of data access, enabling predictive retrieval of data leading to reduction of cache miss overheads. Iterating over an array of reference types will be worse, because although access to the array itself is predictable and localized, access to the referenced objects is not. A linked structure is worse still; memory access is in general not predictable or localized at all, so cache misses will typically be frequent and expensive. Every further level of indirection increases the likelihood of cache misses and, even more expensively, of page faults.

# Instruction Count and the O-notation

Although, as we observed in the previous section, memory usage can no longer be separated cleanly from execution time, there is still merit in trying to make a separate estimate of the latter. Traditionally, execution time was assumed to be proportional to the count of CPU operations needed to complete a program. This assumption is also subject to some qualifications, which we’ll discuss at the end of this section. First, though, how is the execution count estimated? Detailed analysis can be complex. A relatively simple example is provided in Donald Knuth’s classic book Sorting and Searching ([Knuth98]), where the worst-case execution time for a multiple list insertion sort program on Knuth’s notional MIX machine is derived as

$$
3. 5 N ^ {2} + 2 4. 5 N + 4 M + 2
$$

where $N$ is the number of elements being sorted and M is the number of lists.

As a shorthand way of describing algorithm efficiency, this isn’t very convenient. Clearly we need a broader brush for general use. The one most

commonly used is the O-notation (pronounced “big-oh notation”). The Onotation is a way of describing the performance of an algorithm in an abstract way, without the detail required to predict the precise performance of a particular program running on a particular machine. the main reason for using it is that it gives us a way of describing how the execution time for an algorithm depends on the size of its data set, provided the data set is large enough. For example, in the previous expression the first two terms are comparable for low values of $N$ ; in fact, for $N < 8$ , the second term is larger. But as $N$ grows, the first term increasingly dominates the expression and, by the time it reaches 100, the first term is 15 times as large as the second one. Using a very broad brush, we say that the worst case for this algorithm takes time $O ( N ^ { 2 } )$ . We don’t care too much about the coefficient because that doesn’t make any difference to the single most important question we want to ask about any algorithm: what happens to the execution count when the data size increases—say, when it doubles? For the worst-case insertion sort, the answer is that the execution count goes up fourfold. That makes $O ( N ^ { 2 } )$ pretty bad—worse than any we will meet in practical use in this book.

Table 5-1. Some common Big-O classes   

<table><tr><td>Time</td><td>Common name</td><td>Effect on the execution count if N is doubled</td><td>Example algorithms</td></tr><tr><td>O(1)</td><td>Constant</td><td>Unchanged</td><td>Insertion into a hash table (“Set Implementations”)</td></tr><tr><td>O(log N)</td><td>Logarithmic</td><td>Increased by a constant amount</td><td>Insertion into a tree (“TreeSet”)</td></tr><tr><td>O(N)</td><td>Linear</td><td>Doubled</td><td>Linear search</td></tr><tr><td>O(N log N)</td><td></td><td>Doubled plus an amount proportional to N</td><td>Merge sort (“Changing the Order of List Elements”)</td></tr><tr><td>O(N2)</td><td>Quadratic</td><td>Increased fourfold</td><td>Bubble sort</td></tr></table>

Table 5-1 shows some commonly found execution counts, together with examples of algorithms to which they apply. For example, many other execution counts are possible, including some that are much worse than those in the Figure. Many important problems can be solved only by algorithms that take $O ( 2 ^ { N } ) \cdot$ —for these, when N doubles, the execution count is squared! For all but the smallest data sets, such algorithms are infeasibly slow.

Sometimes we have to think about situations in which the cost of an operation varies with the state of the data structure. For example, adding an element to the end of an ArrayList can normally be done in constant time, unless the ArrayList has reached its capacity. In that case, a new and larger array must be allocated, and the contents of the old array transferred into it. The cost of this operation is linear in the number of elements in the array, but it happens relatively rarely. In situations like this, we calculate the amortized cost of the operation—that is, the total cost of performing it n times divided by n, taken to the limit as n becomes arbitrarily large. In the case of adding an element to an ArrayList, the total cost for $N$ elements is $O ( N )$ , so the amortized cost is O(1).

Big-O analysis of execution counts is often very useful for a broad-brush comparisons of the running times of programs and, for that reason, each interface chapter of this book includes big-O analysis of common operations for each implementation. All the same, there are reasons for caution in treating O-numbers as a proxy for running times. They include:

the assumption in determining the dominant term is that the data size will always be sufficiently large to outweigh constant factors and multipliers; for example, $O ( N + c )$ is taken to be $O ( N )$ for constant $c$ , on the assumption that in situations where it matters, $N > > c$ . This may not be true for very large factors;   
machine instructions do not all have the same execution time; for example, inserting an element at the first position of an ArrayList has complexity $O ( N )$ , because every element must be moved to a position one place higher in the array. But if it is possible for that operation to be executed with a single hardware instruction, then the actual execution time may compare favorably with the same operation on a LinkedList, even though there it has complexity of O(1);   
parallelism complicates the picture still further. With instruction-level parallelism, instructions are processed in stages, with the different stages of successive instructions being executed simultaneously. So the elapsed execution time of a program is normally much less than the

summed execution times of its individual instructions. The same applies, on a larger scale to thread-level parallelism, in which different threads can process their tasks simultaneously except when they are in contention for some resource.

In conclusion, it is worth repeating the value of theoretical performance analysis, including O-numbers: it can provide valuable guidance at the design stage. You would never choose an $O ( N ^ { 2 } )$ algorithm over an $O ( N )$ one for a large data set. But if you have a running system with a performance problem, there is no substitute for accurate measurement to compare different candidate implementations.

# Immutability and Unmodifiability

Functional programming is an attractive style; at their best, functional programs are elegant and demonstrably sound, much more so than objectoriented programs. For this reason, some of the features of functional languages that provide these advantages have been gradually adopted into Java, starting with generics, continuing with streams and lambdas (see “Lambdas and Streams”), and culminating with pattern-matching, at the time of this writing still a work in progress. One important feature of the functional style is that its data structures are immutable—that is, their state cannot be modified after their creation. Immutability confers a number of advantages on a program:

Immutable objects are thread-safe   
Immutable objects are perfectly encapsulated, so removing the need for defensive copying   
Immutability guarantees stable lookup in keyed and ordered collections (see “Contracts”)   
Immutability reduces the number of states that a program can be in, making it simpler, clearer, and easier to understand and reason about

Realising these advantages in a Java program is difficult, however. For an object to be truly immutable, its entire object graph—that is, the object itself and everything it refers to, directly or indirectly—must not observably change after construction. Obviously, immutable graph components like wrapper objects and strings present no problem, but for mutable components the often difficult (and rarely achieved) requirement is that the graph must have guaranteed exclusive access to them.

This has led to alternative conflicting terminologies for describing immutability of collections:

Frameworks like Guava and Eclipse collections refer to immutability of an entire object graph as deep immutability. They refer to a collection that refuses modification at the first level—that is, an attempt to add, remove, or replace an element—as shallow immutability, or often just immutability.   
The Java collections documentation uses immutability to mean immutability of the entire object graph. What the other frameworks refer to as “shallow immutability”, the Java documentation—and this book—call unmodifiability.

Unmodifiability is a kind of partial immutability that doesn’t fully confer any of the advantages of immutability listed above, so you might well question its usefulness. But it does confer real advantages:

Collections of immutable objects, including wrapper objects and strings, are not uncommon, and unmodifiable collections of these provide the full benefits of immutability.   
Even partial immutability reduces the number of program states that you have to consider when understanding a program and reasoning about its correctness.   
Because unmodifiable sets and maps can be backed by arrays instead of hashed structures, they can provide very significant space savings.

Chapter [Link to Come] discusses further the situations in which you would choose to use unmodifiable collections.

# Contracts

In reading about software design, you are likely to come across the term contract, often without any accompanying explanation. In fact, software engineering gives this term a meaning that is very close to what people usually understand a contract to be. In everyday usage, a contract defines what two parties can expect of each other—their obligations to each other in some transaction. If a contract specifies the service that a supplier is offering to a client, the supplier’s obligations are obvious. But the client, too, may have obligations—including, but not only, the obligation to pay— and failing to meet them will automatically release the supplier from their obligations as well. For example, airlines’ conditions of carriage—for the class of tickets that your authors can afford, anyway—release them from the obligation to carry passengers who have failed to turn up on time. This allows the airlines to plan their service on the assumption that all the passengers they are carrying are punctual; they do not have to incur extra work to accommodate clients who have not fulfilled their side of the contract.

Contracts work the same way in software. If the contract for a method lays down preconditions on its arguments (i.e. the obligations that a client must fulfill), the method is required to return its contracted results only when those preconditions are fulfilled. For example, binary search (see “Finding Specific Values in a List”) is a fast algorithm to find a key within an ordered list, and it fails if you apply it to an unordered list. So the contract for Collections.binarySearch can say, “if the list is unsorted, the results are undefined”, and the implementer of binary search is free to write code which, given an unordered list, returns random results, throws an exception, or even enters an infinite loop. In practice, this situation is relatively rare in the contracts of the core API because, instead of restricting input validity, they mostly allow for error states in the preconditions and

specify the exceptions that the method must throw if it gets bad input. This design is appropriate for general libraries such as the Collections Framework, which will be very heavily used in widely varying situations by programmers of widely varying ability. APIs of less general libraries usually avoid it, because it restricts the flexibility of the supplier unnecessarily. In principle, all that a client should need to know is how to keep to its side of the contract; if it fails to do that, all bets are off and there should be no need to say exactly what the supplier will do.

It’s good practice in Java to code to an interface rather than to a particular implementation, so as to provide maximum flexibility in choosing implementations. For that to work, what does it imply about the behavior of implementations? If your client code uses methods of the List interface, for example, and at run time the object doing the work is actually an ArrayList, you would like to know that the assumptions you have made about how Lists behave are true for ArrayLists also. So a class implementing an interface usually has to fulfill all the obligations laid down by the terms of the interface contract. (A weaker form of these obligations is already imposed by the compiler: a class claiming to implement an interface must provide concrete method definitions matching the declarations in the interface, but contracts take this further by specifying the behavior of these methods as well.)

The obligations imposed by the Collections Framework interfaces are unusual in some respects, however. A guiding principle in the design of the Framework was that it should be simple—a vital characteristic in a library that every Java programmer must learn. Frameworks that separate modifiable from unmodifiable interfaces have many more types than the Java Collections Framework, so the Framework designers decided to avoid this separation. But since some collection views (and, since Java 9, unmodifiable collections also) do not support all write operations, the interface contracts label these operations optional. For example, the Set view of a Map’s keys can have elements removed but not added (see Chapter 11), while other view collections can have elements neither added nor removed (e.g., the list view returned by Arrays.asList), or support

no modification operations at all, like collections that have been wrapped in an unmodifiable wrapper (see “Unmodifiable Collections”). The interface contracts provide for implementations to throw

UnsuppportedOperationException when optional methods are called. This feature of the Java Collections Framework is sometimes criticised as contravening the so-called Liskov Substitution Principle (LSP), named for the pioneering computing scientist Barbara Liskov. This “principle” is not in fact an unbreakable principle, but a description of systems in which any use of a supertype, such as an interface, can be replaced by an instance of one of its subtypes, such as an implementing class, without changing the meaning of the program. Actually, the fact that the interface contracts specify write operations as optional means that the LSP does indeed describe the behavior of the views and unmodifiable collections. (Critics of the Collections Framework will dismiss this argument as hairsplitting.)

A problem with this approach is that a client programmer, passed a collection of some interface type, cannot be confident that it will support optional operations without throwing

UnsupportedOperationException. A safe rule of thumb for handling collections is to avoid writing to them unless you know the implementing type, usually because you yourself own the collection. We’ll discuss this guidance further in [Link to Come], and in the chapter [Link to Come] we’ll attempt to evaluate the decisions that led to the design of the Framework as it is today.

Although until now we have only discussed functional requirements, contracts can also require performance guarantees. To fully understand the performance characteristics of a class, however, you often need to know detail about the algorithms of the implementation. In this Chapter, while we concentrate mainly on contracts and how as a client programmer you can make use of them, we also provide further implementation detail from the platform classes where it might be of interest. This can be useful in deciding between implementations, but remember that it is not stable; while contracts are binding, one of the main advantages of using them is that they allow

implementations to change as better algorithms are discovered or as hardware improvements change their relative merits. And of course, if you are using another implementation, algorithm details not governed by the contract may be entirely different.

# Content-based Organization

The discussion of sequenced collections (“Sequenced Collections”) introduced the idea of internally ordered sequences, in which the elements are automatically positioned according to the order of their contents, as defined by some ordering relation (see Chapter 2). These are not the only collections classes to organise their elements automatically according to their contents: hashed collections, priority queues and delay queues have the same property. In the cases of internally ordered sequences and of the queues, the content-based organization is observable via the operations of the collection; not so in the case of the hashed collections, but their organization is used when elements need to be located. What all these collections have in common is an implicit invariant: for them to operate correctly, the value of the fields used to position the elements must not change once they have been inserted into the collection. For example, consider a different version of the class Person, this time defined with a hashCode method, but one that depends on the value of the mutable field name:

```java
class Person {
    private String name;
    public Person(String name) {
        this.name = name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int hashCode() {
        return name.hashCode();
    }
    public boolean equals(Object o) {
        return o instanceof Person p && name.equals(p.name); 
```

```txt
} 
```

Now an object of the type Person can be reliably retrieved, but only if the value of name is unchanged. If that field is modified, the collection can no longer match the object with either the old or the new value:

```txt
Set<Person> people = newHashSet<>();  
Person alice = new Person("Alice");  
people.add(alice);  
moe.setName("Bob");  
assert! people.contains(new Person("Alice"));  
assert! people.contains(new Person("Bob")); 
```

To avoid problems like this, the best guideline is this: whenever you are storing objects in a Set, a Map, or an internally ordered Queue, ensure that the fields used by the collection to organize its contents are immutable.

# Lambdas and Streams

We saw, in “Immutability and Unmodifiability”, that the advantages of the functional programming style have led object-oriented languages towards adoption of many functional language features. Following the introduction of generics in Java 5, the next big innovation came in Java 8, with the introduction of lambdas and streams. Lambdas introduced a way in which functions could be represented in Java programs. For example, the statement:

```javascript
Function<Integer, Integer> doubleInt = x -> x * 2; 
```

declares a function doubleInt, which takes an argument of type Integer and returns the Integer result of doubling it.

The importance of lambdas for collections and collection processing comes from their use in stream operations. Streams are a mechanism for transporting a sequence of values from a source to a destination through a series of operations, typically implemented as lambdas, each of which can

transform, drop, or insert values on the way. This provides an alternative model to the traditional use of collections for aggregate data processing. A simple, if rather contrived, example illustrates the change in thinking. (Taken from the introduction to Mastering Lambdas, by Maurice Naftalin (Oracle Press, 2015)). The record Point represents a point defined by its x and y co-ordinates, with a single method distanceFrom that calculates its distance from another point.

```txt
record Point(int x, int y) { public double distanceFrom(Point p) { ... } } 
```

Without streams, a common model of bulk data processing is to process collections in a series of stages: a collection is iteratively processed to produce a new collection, which in turn is iteratively processed, and so on. The following code starts with a collection of Integer instances, applies an arbitrary transformation to produce a set of Point instances, and finally finds the maximum among the distances of each Point from the origin.

```txt
Point origin = new Point(0, 0);  
List<integer> ArrayList = Arrays.asList(1, 2, 3, 4, 5);  
List<point> pointList = new ArrayList<>();  
for (Integer i : ArrayList) {  
    pointList.add(new Point(i % 3, i / 3));  
}  
double maxDistance = Double.MIN_VALUE;  
for (Point p : pointList) {  
    maxDistance = Math.max(pdistanceFrom.origin), maxDistance);  
} 
```

The real code for which this is a model has several disadvantages: it is very verbose; the intermediate collection pointList is an overhead on the operation of the program, resulting in increased garbage collection costs or even in heap space exhaustion; there is an implicit assumption, dif lt to spot, that the minimum value of an empty list is Double.MIN_VALUE; perhaps worst of all, the intent of the program is hard to discern, because the crucial operations are interspersed with the code for collection handling.

Here is the equivalent code, expressed as a pipeline processing the original collection intList through a series of transformations:

```txt
Point origin = new Point(0, 0);  
List<String> myList = Arrays.asList(1, 2, 3, 4, 5);  
OptionalDouble maxDistance =  
    myList.stream().map(i -> new Point(i % 3, i / 3))  
    .mapToDouble(p -> p_distanceFrom.origin()))  
    .max(); 
```

This example shows in miniature the advantages of stream code: it is more concise and readable, uses less intermediate storage, handles an empty source gracefully, and can never attempt to mutate the source collection.

From here on in this book, where example collection code can benefit from partial conversion to the use of streams, we will show stream code as an alternative in a sidebar.

# Collections and Thread Safety

When a Java program is running, it is executing one or more execution streams, or threads. A thread is like a lightweight process, so a program simultaneously executing several threads can be thought of as a computer running several programs simultaneously, but with one important difference: different threads can simultaneously access the same memory locations and other system resources. On machines with multiple processors, truly concurrent thread execution can be achieved by assigning a processor to each thread. If, however, there are more threads than processors—the usual case—multithreading is implemented by time slicing, in which a processor executes some instructions from each thread in turn before switching to the next one.

There are two good reasons for using multithreaded programs. An obvious one, in the case of multicore and multiprocessor machines, is to share the work and get it done quicker. (This reason is becoming ever more compelling as hardware designers turn increasingly to parallelism as the

way of improving overall performance.) A second one is that two operations may take varying, perhaps unknown, amounts of time, and you do not want the response to one operation to await the completion of the other. This is particularly true for a graphical user interface GUI), where the response to the user clicking a button should be immediate, and should not be delayed if, say, the program happens to be running compute-intensive part of the application at the time.

Although concurrency may be essential to achieving good performance, it comes at a price. Different threads simultaneously accessing the same memory location can produce unexpected results, unless you take care to constrain their access. Consider Example 5-1, in which the class ArrayStack uses an array and an index to implement the interface Stack, which models a stack of int (despite the similarity of names, this example is different from [Link to Come]). For ArrayStack to work correctly, the variable index should always point at the top element of the stack, no matter how many elements are added to or removed from the stack. This is an invariant of the class. Now think about what can happen if two threads simultaneously attempt to push an element on to the stack. As part of the push method, each will execute the lines //1 and //2, which are correct in a single-threaded environment but in a multi-threaded environment may break the invariant. For example, if thread A executes line //1, thread B executes line //1 and then line //2, and finally thread A executes line //2, only the value added by thread B will now be on the stack, and it will have overwritten the value added by thread A. The stack pointer, though, will have been incremented by two, so the value in the top position of the stack is whatever happened to be there before. This is called a race condition, and it will leave the program in an inconsistent state, likely to fail because other parts of it will depend on the invariant being true.

# Example 5-1. A non-thread-safe stack implementation

```java
interface Stack { public void push(int elt); public int pop(); public boolean isEmpty(); 
```

}   
Stack   
class ArrayList implements Stack{ private final int MAX_ELEMENTS $= 10$ private int[] stack; private int index; public ArrayList(){ stack $=$ new int[MAX_ELEMENTS]; index $= -1$ 1   
public void push(int elt){ if(index $! =$ stack.length-1）{ index++; //1 stack[index] $=$ elt; //2 } else { throw new IllegalStateException("stack overflow"); }   
public int pop() { if(index $! = -1$ ）{ return stack[index]; index--; }else{ throw new IllegalStateException("stack underflow"); }   
public boolean isEmpty() { return index $= = -1$ ；

The increasing importance of concurrent programming during the lifetime of Java has led to a corresponding emphasis in the collections library on flexible and efficient concurrency policies. As a user of the Java collections, you need a basic understanding of the concurrency policies of the different collections in order to know how to choose between them and how to use them appropriately. In this section, we’ll briefly outline the different ways in which the Framework collections handle concurrency, and the implications for the programmer. For a full treatment of the general theory of concurrent programming, see Concurrent Programming in Java ([Lea99]), and for more detail about concurrency in Java and the collections implementations, see Java Concurrency in Practice ([Goetz06]).

# Synchronization and the Legacy Collections

Code like that in ArrayStack is not thread-safe—it works when executed by a single thread, but may break in a multi-threaded environment. Since the incorrect behavior we observed involved two threads simultaneously executing the push method, we could change the program to make that impossible. Using synchronized to modify the declaration of the push method will guarantee that once a thread has started to execute it, all other threads are excluded from that method until the execution is done:

public synchronized void push(int elt) { ... }

This is called synchronizing on a critical section of code, in this case the whole of the push method. Before a thread can execute synchronized code, it has to get the lock on some object—by default, as in this case, the current object. While a lock is held by one thread, another thread that tries to enter any critical section synchronized on that lock will block—that is, will be suspended—until it can get the lock. This synchronized version of push is thread-safe; in a multi-threaded environment, each thread behaves consistently with its behavior in a single-threaded environment.To safeguard the invariant and make ArrayStack as a whole thread-safe, the methods pop and isEmpty must also be synchronized on the same object. The method isEmpty doesn’t write to shared data, so synchronizing it isn’t required to prevent a race condition, but for a different reason. Each thread may use a separate memory cache, which means that writes by one thread may not be seen by another unless either they both take place within blocks synchronized on the same lock, or unless the variable is marked with the volatile keyword.

Full method synchronization was, in fact, the policy of the collection classes provided in JDK1.0: Vector, Hashtable, and their subclasses; all methods that access their instance data are synchronized. These are now regarded as legacy classes to be avoided because of the high price this policy imposes on all clients of these classes, whether they require thread

safety or not. Synchronization can be very expensive: forcing threads to queue up to enter the critical section one at a time slows down the overall execution of the program, and the overhead of administering locks can be very high if they are often contended.

# Java 2: Synchronized Collections and Fail-Fast Iterators

The performance cost of internal synchronization in the JDK 1.0 collections led the designers to avoid it when the Collections Framework was first introduced in JDK 1.2. Instead, the platform implementations of the interfaces List, Set, and Map widened the programmer’s choice of concurrency policies. To provide maximum performance for singlethreaded execution, the new collections provided no concurrency control at all. (More recently, the same policy change has been made for the synchronized class StringBuffer, which was complemented in Java 5 by its unsynchronized equivalent, StringBuilder.)

Along with this change came a new concurrency policy for collection iterators. In multithreaded environments, a thread which has obtained an iterator will usually continue to use it while other threads modify the original collection. So iterator behavior has to be considered as an integral part of a collection’s concurrency policy. The policy of the iterators for the Java 2 collections is to fail fast, as described in “Iterable and Iterators”: every time they access the backing collection, they check it for structural modification (which, in general, means that elements have been added or removed from the collection). If they detect structural modification, they fail immediately, throwing ConcurrentModificationException rather than continuing to attempt to iterate over the modified collection with unpredictable results. Note that this fail-fast behavior is provided to help find and diagnose bugs; it is not guaranteed as part of the collection contract.

The appearance of Java collections without compulsory synchronization was a welcome development. However, thread-safe collections were still required in many situations, so the Framework provided an option to use the

new collections with the old concurrency policy, by means of synchronized wrappers (see Chapter 12). These are created by calling one of the factory methods in the Collections class, supplying an unsynchronized collection which it will encapsulate. For example, to make a synchronized List, you could supply an instance of ArrayList to be wrapped. The wrapper implements the interface by delegating method calls to the collection you supplied, but the calls are synchronized on the wrapper object itself. Example 5-2 shows a synchronized wrapper for the interface Stack of Example 5-1. To get a thread-safe Stack, you would write:

Stack threadSafe $=$ new SynchronizedArrayStack(new ArrayStack());

This is the preferred idiom for using synchronized wrappers; the only reference to the wrapped object is held by the wrapper, so all calls on the wrapped object will be synchronized on the same lock—that belonging to the wrapper object itself. It’s important to have the synchronized wrappers available, but don’t use them more than you have to, because they suffer the same performance disadvantages as the legacy collections.

Example 5-2. A synchronized wrapper for ArrayStack   
public class SynchronizedArrayStack implements Stack { private final Stack stack; public SynchronizedArrayStack(Stack stack) { this.stack $=$ stack; } public synchronized void push(int elt){ stack.push(elt);} public synchronized int pop(){ return stack.pop();} public synchronized boolean isEmpty(){ return stack.isEmpty();}

Using Synchronized Collections Safely Even a class like SynchronizedArrayStack, which has fully synchronized methods and is itself thread-safe, must still be used with care in a concurrent environment. For example, this client code is not thread-safe:

```javascript
Stack stack = new SynchronizedArrayStack(new ArrayList());  
// don't do this in a multi-threaded environment  
if (!stack.isEmpty()) { 
```

```txt
stack.pop(); // can throw IllegalStateException; } 
```

The exception would be raised if the last element on the stack were removed by another thread in the time between the evaluation of isEmpty and the execution of pop. This is an example of a common concurrent program bug, sometimes called test-then-act, in which program behavior is guided by information that in some circumstances will be out of date. To avoid it, the test and action must be executed atomically. For synchronized collections (as for the legacy collections), this must be enforced with clientside locking:

```txt
synchronized stack) { if (!stack.isEmpty()) { stack.pop(); } } 
```

For this technique to work reliably, the lock that the client uses to guard the atomic action should be the same one that is used by the methods of the synchronized wrapper. In this example, as in the synchronized collections, the methods of the wrapper are synchronized on the wrapper object itself. (An alternative is to confine references to the collection within a single client, which enforces its own synchronization discipline. But this strategy has limited applicability.)

Client-side locking ensures thread-safety, but at a cost: since other threads cannot use any of the collection’s methods while the action is being performed, guarding a long-lasting action (say, iterating over an entire array) will have an impact on throughput. This impact can be very large if the synchronized methods are heavily used; unless your application needs a feature of the synchronized collections, such as exclusive locking, the Java 5 concurrent collections are almost always a better option.

# Java 5: Concurrent Collections

Java 5 introduced thread-safe concurrent collections as part of a much larger set of concurrency utilities, including primitives—atomic variables and locks—which give the Java programmer access to relatively recent hardware innovations for managing concurrent threads, notably compareand-swap operations, explained below. The concurrent collections remove the necessity for client-side locking as described in the previous section—in fact, external synchronization is not even possible with these collections, as there is no one object which when locked will block all methods. Where operations need to be atomic—for example, inserting an element into a Map only if it is currently absent—the concurrent collections provide a method specified to perform atomically—in this case,

ConcurrentMap.putIfAbsent.

If you need thread safety, the concurrent collections generally provide much better performance than synchronized collections. This is primarily because their throughput is not reduced by the need to serialize access, as is the case with the synchronized collections. Synchronized collections also suffer the overhead of managing locks, which can be high if there is much contention. These differences can lead to efficiency differences of two orders of magnitude for concurrent access by more than a few threads.

# Mechanisms

The concurrent collections achieve thread-safety by several different mechanisms. The first of these—the only one that does not use the new primitives—is copy-on-write. Classes that use copy-on-write store their values in an internal array, which is effectively immutable; any change to the value of the collection results in a new array being created to represent the new values. Synchronization is used by these classes, though only briefly, during the creation of a new array; because read operations do not need to be synchronized, copy-on-write collections perform well in the situations for which they are designed—those in which reads greatly predominate over writes. Copy-on-write is used by

the collection classes CopyOnWriteArrayList and CopyOnWriteArraySet.

A second group of thread-safe collections relies on compare-and-swap (CAS), a fundamental improvement on traditional synchronization. To see how it works, consider a computation in which the value of a single variable is used as input to a long-running calculation whose eventual result is used to update the variable. Traditional synchronization makes the whole computation atomic, excluding any other thread from concurrently accessing the variable. This reduces opportunities for parallel execution and hurts throughput. An algorithm based on CAS behaves differently: it makes a local copy of the variable and performs the calculation without getting exclusive access. Only when it is ready to update the variable does it call CAS, which in one atomic operation compares the variable’s value with its value at the start and, if they are the same, updates it with the new value. If they are not the same, the variable must have been modified by another thread; in this situation, the CAS thread can try the whole computation again using the new value, or give up, or—in some algorithms— continue, because the interference will have actually done its work for it! Collections using CAS include ConcurrentLinkedQueue and ConcurrentSkipListMap.

The third group uses implementations of java.util.concurrent.locks.Lock, an interface introduced in Java 5 as a more flexible alternative to classical synchronization. A Lock has the same basic behavior as classical synchronization, but a thread can also acquire it under special conditions: only if the lock is not currently held, or with a timeout, or if the thread is not interrupted. Unlike synchronized code, in which an object lock is held while a code block or a method is executed, a Lock is held until its unlock method is called (making it possible for a Lock to be released in a different block or method from that in which it was acquired). Some of the collection classes in this group make use of these features to divide the collection into parts that can be separately locked, giving improved concurrency. For example,

LinkedBlockingQueue has separate locks for the head and tail ends of the queue, so that elements can be added and removed in parallel. Other collections using these locks include ConcurrentHashMap and most of the implementations of BlockingQueue.

Iterators The mechanisms described above lead to iterator policies more suitable for concurrent use than fail-fast, which implicitly regards concurrent modification as a problem to be eliminated. Copy-on-write collections have snapshot iterators. These collections are backed by arrays which, once created, are never changed; if a value in the collection needs to be changed, a new array is created. So an iterator can read the values in one of these arrays (but never modify them) without danger of them being changed by another thread. Snapshot iterators do not throw ConcurrentModificationException.

Collections which rely on CAS have weakly consistent iterators, which reflect some but not necessarily all of the changes that have been made to their backing collection since they were created. For example, if elements in the collection have been modified or removed before the iterator reaches them, it definitely will reflect these changes, but no such guarantee is made for insertions. Weakly consistent iterators also do not throw ConcurrentModificationException.

The third group described above also have weakly consistent iterators. In Java 6 this includes DelayQueue and PriorityBlockingQueue, which in Java 5 had fail-fast iterators. This means that you cannot iterate over the Java 5 version of these queues unless they are quiescent, at a time when no elements are being added or inserted; at other times you have to copy their elements into an array using toArray and iterate over that instead.

# Chapter 6. The Collection

# Interface

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 12th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

The interface Collection<E> defines the core functionality that we expect of any collection other than a map. It provides methods in four groups: adding elements, removing them, querying the collection’s contents, and making its elements available for further processing.

# Adding Elements

```txt
boolean add(E e) // adds the element e  
boolean addAll(Collection<? extends E> c) // adds the contents of c 
```

The contracts for these methods specify that after their execution, the collection must contain the element or elements suppied in the argument. So if a method call fails because, for example, it has attempted to add null to a null-hostile collection, it must throw an exception. But calls can succeed without changing the contents of the collection if, for example, they attempt

to add an element to a set that already contains it. In this case, the result returned will be false, indicating that the collection was unchanged by the call.

To maintain the type uniformity of the collection, these methods only allow you to add elements, or element collections, of the parametric type. This is in contrast to the methods of the next group:

# Removing Elements

```txt
void clear() // removes all elements  
boolean remove(Object o) // removes an element o  
boolean removeAll(Collection<?> c) // removes all occurrences  
of the elements in c  
boolean retainAll(Collection<?> c) // removes the elements  
*not* in c  
boolean removeIf(Predicate<? super E> p) // removes the elements  
for which p is true 
```

Apart from clear, which empties the collection, the methods in this group, as in the first one, return a boolean result indicating whether the collection was changed by their action.

If the argument o to remove is null, the method will remove a single null from the collection if one is present. Otherwise, if an element e is present for which o.equals(e), it removes it; if not, it leaves the collection unchanged. The behavior of removing a single element is in contrast to removeAll, which removes all occurrences in this collection of every matching element in the supplied collection.

In contrast to the methods for adding elements, these methods—and those of the next group—will accept as arguments elements or element collections of any type. We’ll discuss this design choice later, in the section “Using the Methods of Collection”.

# Querying the Contents of a Collection

```txt
boolean contains(Object o) // returns true if o is present  
boolean containsAll(Collection<?> c) // returns true if all elements of c // are present in the collection  
boolean isEmpty() // returns true if no elements are present  
int size() // returns the element count (or // Integer.MAX_VALUE if that is less) 
```

The decision to make size return Integer.MAX_VALUE for extremely large collections was probably taken on the assumption that collections this large—with more than two billion elements—will rarely occur. Even so, an alternative design, throwing an exception instead of returning an arbitrary value, would have the advantage of ensuring that the contract for size could clearly state that if it does succeed in returning a value, that value will be correct.

# Making a Collection’s Contents Available for Further Processing

```txt
Iterator<E> iterator() // returns an Iterator over the elements  
Splitsor<E> splitterator // returns a Splitator over the elements  
Stream<E> stream() // returns a sequential Stream with this as its source  
Stream<E> parallelStream() // returns a parallel Stream with this as its source  
Object[] toArray() // copies contents to an Object[ ]  
<T> T[] toArray(T[] t) // copies contents to a T[ ] (for any T)  
<T> T[] toArray(IntFunction<T] > generator) // copies contents to a T[ ], created by the generator 
```

Description for Callout 1   
0 Description for Callout 2   
② Description for Callout 3   
③

The first two methods in this group create objects that make the collection’s elements available for processing sequentially or in parallel, as described in (new preliminaries section). As that section explains, the most common use of Spliterators is internally in one of the next two methods, stream and parallelStream, which support the functional-style processing of stream elements.

The last three methods, different overloads of toArray, all return an array containing the elements of this collection. Overload creates a new Object[], while and take a T[]—or, respectively, a function producing a T[] of a given size—and return a T[] containing the elements of the collection. These methods are important because many APIs, principally older ones and those for which performance is especially important, expose methods that accept or return arrays.

As discussed in [Link to Come], the arguments of the last two methods are required in order to provide the virtual machine with the reifiable type of the array. The new array will be created during the method execution, although if the array supplied as the argument to is long enough, it is used to receive the elements of the collection, overwriting its existing elements. Reusing a supplied array in this way can be more efficient, particularly if the method is being called repeatedly. But if instead you are creating a new array, a common and straightforward idiom is to supply an array of zero length:

```txt
Collection<String> cs = ...  
String[] sa = cs.toArray(String):::new); 
```

which, in Collection’s default implementation of $\otimes$ , results in a call to with an argument of length 0:

```txt
Collection<String> cs = ...  
String[] sa = cs.toArray(new String[0]); 
```

If $\otimes$ or is being called repeatedly in this way, a more efficient alternative is to declare a single empty array of the required type, that can then be used

as many times as required:

```java
private static final String[] EMPTY_STRING_ARRAY = new String[0];  
Collection<String> cs = ...  
String[] sa = cs.toArray(EMPTY_STRING_ARRAY); 
```

Why is any type allowed for T in the declaration of and , rather than restricting the type to E, the parametric type of the collection? One reason is to allow the possibility of giving the array a more specific component type than that of the collection, when the elements of the collection all happen to belong to the same subtype:

```javascript
List<object> 1 = List.of("zero","one"); String[] a = 1.toArray(new String[0]); 
```

Here, a list of objects happens to contain only strings, so it can be converted into a String[], in an operation analogous to the promote method described in [Link to Come]. If the list contains an object that is not a string, the error is caught at run time rather than compile time:

```txt
List<object> 1 = List.of("zero","one",2); String[] a = 1.toArray(new String[0]); //throws ArrayStoreException (see §2.5) 
```

In general, you may want to copy a collection of a given type into an array of a more specific type (for instance, copying a list of objects into an array of strings, as just shown), or of a more general type (for instance, copying a list of strings into an array of objects). You would never want to copy a collection of a given type into an array of a completely unrelated type (for instance, copying a list of integers into an array of strings is always wrong). But since there’s no way to specify this constraint in Java, such errors are caught at run time rather than compile time.

One drawback of this design is that, applied to collections of wrapper types, it doesn’t accommodate automatic unboxing into the corresponding array of primitives:

```typescript
List<String> 1 = List.of(0,1,2);  
int[] a = 1.toArray(new int[0]); // compile-time error 
```

This is illegal because the parameter T in the method call must—as for any type parameter—be a reference type. The call would work if we replaced both occurrences of int with Integer, but often this won’t do because, for performance or compatibility reasons, we require an array of primitive type. In such cases, there is nothing for it but to copy the array explicitly:

```matlab
List<Integer> l = List.of(0,1,2);  
int[] a = new int[l.size());  
for (int i=0; i<1.size(); i++) a[i] = l.get(i); 
```

The Stream API provides a neater alternative:

```txt
1.stream().mapToInt(Integer::intValue).ToArray() 
```

# Using the Methods of Collection

Let’s construct a small example to illustrate the use of the collection classes. Your authors are forever trying to get organized; we’ll imagine that our latest effort involves writing our own to-do manager. In this example, we’ll see how to write code in the classical Collections Framework idiom, and also, in sidebars, the more modern functional style of the Stream API.

We begin by defining an interface to represent tasks, and records to represent two different kinds of tasks: writing code and making phone calls.

This is the interface that defines a task:

```java
public interface Task extends Comparable<Task> { default int compareTo(Task t) { return toString().compareTo(t.toString()); } } 
```

and the two concrete Task types are

record CodingTask(String spec) implements Task {} record PhoneTask(String name, String number) implements Task {}

Tasks will need to be comparable to be used in ordered collections (such as OrderedSet and OrderedMap); the other methods that they will require—equals, hashCode, and toString—are automatically supplied by the record implementation. The natural ordering on tasks (see “Comparable<T>”) corresponds to the ordering on their string representations and, as records, equality of two tasks is defined by the equality of their string-valued fields. Since the natural ordering on strings is consistent with equality—that is, compareTo returns 0 exactly when equals returns true—it follows that for tasks also, the natural order is consistent with equality.

A coding task is specified by its name, and a phone task is specified by the name and number of the person to be called. As records whose string components are all immutable, objects of these types are themselves immutable (see ??).

The string representation of a record returned by the default toString method always begins with the name of the record type. Since "CodingTask" precedes "PhoneTask" in the alphabetic ordering on strings, and since tasks are ordered according to the results returned by toString, it follows that coding tasks come before phone tasks in the natural ordering—highly appropriate for people who much prefer coding to talking on the phone!

We also define an empty task:

```java
record EmptyTask() implements Task { public String toString(){ return ""; }   
} 
```

Since the empty string precedes all others in the natural ordering on strings, the empty task comes before all others in the natural ordering on tasks. This task will be useful when we construct range views of sorted sets (see Getting Range Views).

```javascript
PhoneTask mikePhone = new PhoneTask("Mike", "987 6543");
PhoneTask paulPhone = new PhoneTask("Paul", "123 4567");
CodingTask databaseCode = new CodingTask("db");
CodingTask guiCode = new CodingTask("gui");
CodingTask logicCode = new CodingTask("logic"); 
```

```txt
Collection<PhoneTask> phoneTasks = newHashSet<>();  
Collection<CodingTask> codingTasks = newHashSet<>();  
Collection<Task> mondayTasks = newHashSet<>();  
Collection<Task> tuesdayTasks = newHashSet<>(); 
```

```javascript
Collections.addAll phoneTasks, mikePhone, paulPhone); Collections.addAllcodingTasks,/databaseCode, guiCode, logicCode); Collections.addAll(mondayTasks,_logicCode, mikePhone); Collections.addAll(tuesdayTasks,/databaseCode, guiCode, paulPhone); 
```

Using the Stream API, the two blocks of code above can be condensed into a single block:

```javascript
var phoneTasks =   
Stream.of(mikePhone,paulPhone).collect(toSet());   
var codingTasks =   
Stream.of(databaseCode,guiCode,logicCode).collect(toSet());   
var mondayTasks =   
Stream.of(logicCode,mikePhone).collect(toSet());   
var tuesdayTasks =   
Stream.of(databaseCode,guiCode,paulPhone).collect(toSet()); 
```

```txt
assert phoneTasks.equals(Set.of(mikePhone, paulPhone));  
assert codingTasks.equals(Set.of(databaseCode, guiCode, logicCode));  
assert mondayTasks.equals(Set.of(LogicCode, mikePhone)); 
```

assert tuesdayTasks.equals(Set.of(databaseCode, guiCode, paulPhone));

??? shows how we can define a series of tasks to be carried out. (In a real system, of course, tasks would be most likely held in a database and retrieved from there.) As part of the retrieval process, the tasks have been organized into various categories—phone tasks, coding tasks, and so on, represented by sets, using the method Collections.addAll introduced in [Link to Come]. The choice of sets as the specific collection type isn’t altogether arbitrary: although any collection type would have served the purpose, Set is unique in the Framework in having exactly the same methods as the top-level Collection interface that it inherits from. Now we can use the methods of Collection to work with these categories. The examples that follow use the methods in the order in which they were presented earlier.

Adding Elements: We can add new tasks to the schedule:

```javascript
PhoneTask ruthPhone = new PhoneTask("Ruth", "567 1234");
mondayTasks.add(ruthPhone);
assert mondayTasks.equals(Set.of(logicCode, mikePhone, ruthPhone)); 
```

or we can combine schedules together:

```txt
Collection<Task> allTasks = newHashSet<(mondayTasks);  
allTasks.addAll(tuesdayTasks);  
assert allTasks.equals(Set.of(logicCode, mikePhone, ruthPhone, databaseCode, guiCode, paulPhone)); 
```

```javascript
var allTasks = Stream.of(mondayTasks,tuesdayTasks) .flatMap(Collection:::stream) .collect(toSet()); 
```

Removing Elements: When a task is completed, we can remove it from a schedule:

```lisp
boolean wasPresent = mondayTasks.remove(mikePhone);  
assert wasPresent;  
assert mondayTasks.equals(Set.of(logicCode, ruthPhone)) 
```

and we can clear a schedule out altogether because all of its tasks have been done (a method that, in reality, we don’t get to use very often):

```java
mondayTasks.clear();   
assert mondayTasks.equals(Collectionss EMPTY_SET); 
```

The removal methods also allow us to combine entire collections in various ways. For example, to see which tasks other than phone calls are scheduled for Tuesday, we can write:

```txt
Collection<Task> tuesdayNonphoneTasks = newHashSet<> (tuesdayTasks); tuesdayNonphoneTasks.removeAll(phones); assert tuesdayNonphoneTasks.equals(Set.of(databaseCode, guiCode)); 
```

```javascript
var tuesdayNonPhoneTasks = mondayTasks.stream().filter(t -> ! phoneTasks.contains(t)); 
```

or to see which phone calls are scheduled for that day:

```txt
Collection<Task> phoneTuesdayTasks = newHashSet<> (tuesdayTasks);  
phoneTuesdayTasks retainAll( phoneTasks);  
assert phoneTuesdayTasks.equals(Set.of(paulPhone)); 
```

```javascript
var phoneTuesdayTasks = tuesdayTasks.stream()
.filter(telephoneTasks::contains).collect(toSet()); 
```

This last example can be approached differently to achieve the same result:

```dart
Collection<PhoneTask> tuesdayPhoneTasks = newHashSet<> (phoneTasks);  
tuesdayPhoneTasks retainAll(tuesdayTasks);  
assert tuesdayPhoneTasks.equals(Set.of(paulPhone)); 
```

```txt
var tuesdayPhoneTasks = phoneTasks.stream()
.filter(tuesdayTasks::contains).collect(toSet()); 
```

Note that phoneTuesdayTasks has the type Collection<Task>, while tuesdayPhoneTasks has the more precise type Collection<PhoneTask>.

This last example provides an explanation of the signatures of methods in this group and the next. We have already discussed (“Wildcards Versus Type Parameters”) why they take arguments of type Object or Collection<?> whereas the methods for adding to the collection restrict their arguments to its parametric type. Taking the example of retainAll, its contract requires the removal of the elements of this collection which do not occur in the argument collection. That doesn’t justify any restriction on what the argument collection may contain; in the example just given it can contain instances of any kind of Task, not just PhoneTask. There is no reason even to restrict the argument in this way to collections of supertypes of the parametric type; we actually want the least restrictive type possible, which is Collection<?>. Similar reasoning applies to remove, removeAll, contains, and containsAll.

Querying the Contents of a Collection: These methods allow us to check, for example, that the operations above have worked correctly. We’ll use assert here to make the system check our belief that we have programmed the previous operations correctly. For example the first statement will fail, throwing an AssertionError, if tuesdayPhoneTasks does not contain paulPhone:

```txt
assert tuesdayPhoneTasks.contains(paulPhone);
assert tuesdayTasks.containsAll(tuesdayPhoneTasks);
assert mondayTasks.isEmpty();
assert mondayTasks.size() == 0; 
```

# Making the Collection Contents Available for Further Processing:

The methods in this group provide an iterator over the collection, deliver its contents into a stream, or convert it to an array.

“Iterable and Iterators” showed how most uses of iterators can be replaced by the simpler foreach statement, which uses them implicitly. But there are uses of iteration with which foreach can’t help: you have to use an explicit iterator if you want to change the structure of a collection without encountering ConcurrentModificationException, or if you want to process two lists in parallel. For example, suppose that we decide that we don’t have time for phone tasks on Tuesday. It may perhaps be tempting to use foreach to filter them from our task list, but that won’t work for the reasons described in “Iterable and Iterators”:

```txt
// throws ConcurrentModificationException for (Task t : mondayTasks) { if (t instanceof PhoneTask) { mondayTasks.remove(t); } } 
```

Using an iterator explicitly is no improvement if you still use Collection methods to modify the structure:

```java
// throws ConcurrentModificationException for (Iterator<Task> it = mondayTasks.iterator()) ; it.hasNext(); 
```

）{ Task t $=$ it.next(); if (t instanceof PhoneTask) { tuesdayTasks.remove(t); }   
1

But using the iterator’s structure-changing method gives the result we want:

for (Iterator<Task> it = mondayTasks.iterator()) ; it.hasNext();   
）{ Task t $=$ it.next(); if (t instanceof PhoneTask) { it.remove(); }   
}

Compared to this inelegant code, the appeal of the Stream API is obvious:

```javascript
var tuesdayCodeTasks = tuesdayTasks.stream()
.filter(t -> ! (t instanceof PhoneTask))
collect(Collectors.toArray()); 
```

The stream version has the advantages of clarity, brevity, and the possibility of efficiently parallelizing this operation, simply by changing the call of stream to parallelStream. The performance cost of these advantages is the extra memory required to create the new collection.

A more elaborate example of the use of iterators will be shown in another chapter, although there again we’ll see that the Stream API offers a more concise and readable alternative.

# Implementing Collection

There are no concrete implementations of Collection. The class AbstractCollection, which partially implements it, is one of a series of skeletal implementations—including AbstractSet,

AbstractList, and so on—which provide functionality common to the different concrete implementations of each interface. These skeletal implementations are available to help the designer of new implementations of the Framework interfaces. For example, Collection could serve as the interface for bags (unordered lists), and a programmer implementing bags could extend AbstractCollection and find most of the implementation work already done.

# Collection Constructors

We will go on to look at the three main kinds of collection in the next three chapters, but we should first explain two common forms of constructor which are shared by most collection implementations. Taking HashSet as an example, these are:

public HashSet() public HashSet(Collection<? extends E> c)

The first of these creates an empty set, and the second a set that will contain the elements of any collection of the parametric type—or one of its subtypes, of course. Using this constructor has the same effect as creating an empty set with the default constructor, and then adding the contents of a collection using addAll. This is sometimes called a “copy constructor”, but that term should really be reserved for constructors which make a copy of an object of the same class, whereas constructors of the second form can take any object which implements the interface Collection<? extends E>. Joshua Bloch has suggested the term “conversion constructor”.

Not all collection classes have constructors of both forms— ArrayBlockingQueue, for example, cannot be created without fixing its capacity, and SynchronousQueue cannot hold any elements at all, so no constructor of the second form is appropriate. In addition, many collection classes have other constructors besides these two, but which ones they have depends not on the interface they implement but on the underlying implementation; these additional constructors are used to configure the implementation.

OceanofPDF.com

# Chapter 7. The SequencedCollection Interface

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 13th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

The interface SequencedCollection occupies a unique place in the design space of the Framework. Rather than specifying the contract for a particular data structure, it unifies behaviors exposed by a variety of interfaces and one implementation—List, NavigableSet, Deque, and LinkedHashSet—that are spread across the type hierarchy and mostly not otherwise related. What these collections have in common is that all represent a sequence of elements whose order is semantically significant (sometimes called the encounter order), whether the order is internally or externally imposed (see “Sequenced Collections”).

It was Deque (see “Deques”), a relatively late addition to the Collections Framework, that introduced the set of methods that has now been generalised to all collections with an encounter order, providing the ability to add, inspect, and remove the first and last elements of the sequence. Some of these capabilities were previously available, unevenly and under

different names, in these collections: for example, NavigableSet supported the removal of the first and last elements but not their inspection, while List directly supported only addition of a last element, all other operations requiring a sequence position to be specified (see [Link to Come]).

It would be impossible to unify the behavior of so many disparate collections without some special cases. For example, it makes no sense to add a first or last element to a NavigableSet, given that the position of all the elements in these collections is determined by the internal sorting algorithm. For another example, unmodifiable lists cannot support structural modifications such as addition and removal of elements. These special cases will be dealt with in the chapters covering the different collection types.

The single new method provided by SequencedCollection is reversed, which provides a reverse-ordered view (see ??) of the original collection. This makes the ability to process an ordered collection in reverse (previously only available to clients of NavigableSet via its descendingSet method) available to users of any collection with encounter order.

# Adding Elements

```txt
void addFirst(E e) // adds e as the first element of this collection  
void addLast(E e) // adds e as the last element of this collection 
```

The contracts for these methods specify that after their successful execution, the collection must contain the supplied element as the first (respectively last) element in the encounter order.

# Inspecting Elements

E getFirst()

// returns the first element of this

```txt
collection EgetLast( collection 
```

// returns the last element of this

# Removing Elements

```txt
E removeFirst() of this collection E removeLast() of this collection 
```

// removes and returns the first element

// removes and returns the last element

Calling an inspection or a removal method on an empty collection will normally result in a NoSuchElementException.

# View-Generating Method

SequencedCollection<E> reversed()

Returns a reverse-ordered view of this collection. Of course, you would prefer that calling reversed on, for example, a List (an interface that inherits from SequencedCollection) would return a List, rather than a generic SequencedCollection, as the declaration above implies. For this reason, the four interfaces that inherit from SequencedCollection—List, SequencedSet, NavigableSet, and Deque—all define covariant overrides for reversed. So, for example, SequencedSet’s declaration of reversed is

SequencedSet<E> reversed()

Similar overrides are declared by the other three interfaces.

The reverse ordering affects iteration and other order-sensitive operations, including those on views of the returned view. The contract states that any successful modifications to this view must write through to the underlying

collection, but that the inverse—visibility in this view of changes to the underlying collection—are implementation-dependent.

OceanofPDF.com

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 14th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

A set is a collection of items that cannot contain duplicates; adding an item that is already present in the set has no effect. The Set<E> interface has the same methods as those of Collection, but it is defined separately in order to allow the contract of add (and addAll, which is defined in terms of add) to be changed in this way. For example, returning to the task manager, suppose that on Monday you have free time to carry out your telephone tasks. You can make the new collection of tasks for Monday by adding all your telephone tasks to the existing Monday tasks. Let mondayTasks and phoneTasks be as declared in ???. Using a set, you can write:

Set<Task> phoneAndMondayTasks $=$ new HashSet<>(mondayTasks); phoneAndMondayTasks.addAll(phoneTasks); assert phoneAndMondayTasks.equals(Set.of(logicCode, mikePhone, paulPhone));

This works because of the way that duplicate elements are handled. The task mikePhone, although it is in both mondayTasks and

phoneTasks, appears only once in phoneAndMondayTasks— appropriately for this application: you definitely don’t want to have to start doing tasks twice over!

This seems straightforward, but to understand exactly how different Set implementations work, we need to look more closely at what defines a duplicate—that is, the equivalence relation for that set. The example above uses HashSet, for which the equivalence relation is the equals method: in other words, for a HashSet two objects are duplicates if and only if the equals method, called on one with the other as its argument, returns true. This might seem an obvious choice, but it’s not the only one— although the Javadoc (at JDK 20) for Set and its implementations often implies that it is. Some other implementations also use equals, like CopyOnWriteArraySet and the family of classes that we’re calling UnmodifiableSet (see “UnmodifiableSet”). But another choice, if less common, is the identity relation; a set using that relation will contain a reference to every unique object that has been added to it. One such example is EnumSet (see “EnumSet”)—although since Enums are singletons, EnumSet could also be said to use equals. Another is any set created from an IdentityHashMap (see “IdentityHashMap”) using the Collections method newSetFromMap.

A third alternative for an equivalence relation is specified by the contract for NavigableSet. A NavigableSet (see “NavigableSet”) maintains its elements in sorted order using an ordering relation provided by either its natural order or a Comparator (see Chapter 2). NavigableSet also uses that ordering relation in another way: it defines two objects as equivalent if, using it, they compare as equal, regardless of whether they satisfy the equality relation.

The fact that different sets use different equivalence relations creates no difficulties so long as the different relations are compatible (see Chapter 2) —that is, they give the same result applied to every value pair. Confusion arises, however, with incompatible relations; for example, two objects may not satisfy the equality relation even though the set’s ordering relation

compares them as equal. This confusion is unfortunately compounded by the Javadoc for Set and its implementations, which in places fails to recognise that some implementations use equivalence relations other than equals. Hopefully, future Javadoc versions may correct that problem.

You can avoid the confusion by understanding the consequences of using different equivalence relations for different sets. One consequence, obvious once you understand the problem, is that sets may contain duplicate elements satisfying equals or, conversely, that they may elide occurrences of ones that don’t. A less obvious consequence concerns equality between sets: to determine whether two sets A and B are equal, A must test each member of B to discover whether it is equivalent to a member of A. If the roles are reversed, and if A and B are using different equivalence relations, the results may be different, so set equality loses symmetry.

# Set Implementations

When we used the methods of Collection in the examples of Chapter 6, we emphasized that they would work with any implementation of Collection, not only Set, and, even staying with Set, not only the Set implementation HashSet. But in practice, of course, you have to decide on a concrete implementation. This Chapter surveys the different Set implementations provided by the Framework, which differ both in how fast they perform the basic operations of add, contains, and iteration, and in the order in which their iterators return their elements. At the end of the Chapter, we’ll summarize the comparative performance of the different implementations.

Of the Set implementations in the Collections Framework, four directly implement Set itself; the others inherit from the subinterface SequencedSet (see “SequencedSet”. In this section, we will look at these four: HashSet, CopyOnWriteArraySet, EnumSet, and the family of implementations that we’re calling UnmodifiableSet.

# HashSet

This class is the most commonly used implementation of Set. As the name implies, it is implemented by a hash table, an array in which elements are stored at a position derived from their contents. Since hash tables store and retrieve elements by their content, they are well suited to implementing the operations of Set (the Collections Framework also uses them for various implementations of Map). For example, to implement contains(Object o) you would look for the element o and return true if it were found.

An element’s position in a hash table is calculated by a hash function of its contents. Hash functions are designed to give, as far as possible, an even spread of results (hash codes) from the element values that might be stored. For an example, here is code like that used in the String class to calculate a hash code:

int hash $= 0$ for(char ch：str.toArrayarray(){ hash $\equiv$ hash\*31+ch;

Traditionally, hash tables obtain a table index from the hash code by taking the remainder after division by the table length. Division is a slow operation in practice, so the Collections Framework classes use bit masking instead. Since that means it is the pattern of bits at the low end of the hash code that is significant, prime numbers (such as 31, here) are used in calculating the hash code, because multiplying by primes will not tend to shift information away from the low end, as would multiplying by a power of two, for example.

Clearly, unless your table has more locations than there are values that might be stored in it, sometimes two distinct values will hash to the same location in the hash table (this is called a collision). For instance, no intindexed table can be large enough to store all string values without collisions.We can minimize the problem with a good hash function—one that spreads the elements out equally in the table—but, when collisions do

occur, we have to have a way of keeping the colliding elements at the same table location or bucket. This is often done by storing them in a linked structure—a list or a tree—as shown in Figure 8-1. We will look at linked structures in more detail later, but for now it’s enough to see that elements stored at the same bucket can still be accessed, at the cost of following a chain of cell references. Figure 8-1 shows the situation resulting from running this code on the OpenJDK implementation of Java 20:

![](images/c0a8a9afeb2f08ff372c629bc5e56e58d92aeb196b746a08a69a4ef4a2cae5d3.jpg)  
Figure 8-1. A hash table with chained overflow

```typescript
Set<Character> s1 = new Sets<Character>(8);  
s1.add('j');  
s1.add('i');  
s1.add('b'); 
```

The index values of the table elements have been calculated by using the bottom three bits (for a table of length 8) of the hash code of each element. In this implementation, a Character’s hash code is just the Unicode value of the character it contains. (In practice, a hash table would be much bigger than this. Also, this diagram is simplified from the real situation; because HashSet is actually implemented by a specialized HashMap (see “HashMap”), each of the cells in the chain contains not one but two references, to a key and a value (see Chapter 11). Only the key is shown in this diagram because, when a hash table is being used to represent a set, all values are the same—only the presence of the key is significant.)

As long as there are no collisions, the cost of inserting or retrieving an element is constant. As the hash table fills, collisions become more likely; assuming a good hash function, the probability of a collision in a lightly loaded table is proportional to its load, defined as the number of elements in the table divided by its capacity (the number of buckets). If a collision does take place, an overflow structure—a linked list or tree—has to be created and subsequently traversed, adding an extra cost to insertion. If the size of the hash table is fixed, performance will worsen as more elements are added and the load increases. To prevent this from happening, the table size is increased by rehashing—copying to a new and larger table—when the load reaches a specified threshold (its load factor).

Iterating over a hash table requires each bucket to be examined to see whether it is occupied and therefore costs a time proportional to the capacity of the hash table plus the number of elements it contains. Since the iterator examines each bucket in turn, the order in which elements are returned depends on their hash codes, so there is no guarantee as to the order in which the elements will be returned. In the OpenJDK implementation of Java 20, the hash table shown in Figure 8-1 yields its elements in order of ascending table index and forward traversal of the linked lists. Printing it produces the following output.

[i, j, b]

Later in this section we will look at LinkedHashSet, a variant of this implementation with an iterator that does return elements in their insertion order.

The chief attraction of a hash table implementation for sets is the constanttime performance (for lightly-loaded tables) of the basic operations of add, remove, contains, and size. Its main performance disadvantages are the poor performance of heavily-loaded tables, and the iteration performance: iterating through the table involves examining every bucket, so the cost includes a factor attributable to the table length, regardless of the size of the set it contains.

HashSet has the standard constructors that we introduced in “Collection Constructors”, together with two additional constructors:

HashSet(int initialCapacity) HashSet(int initialCapacity, float loadFactor)

Both of these constructors create an empty set but allow some control over the size of the underlying table, creating one with a length of the nextlargest power of 2 after the supplied capacity and, optionally, with the desired load factor. Most of the hash-based maps in the Collections Framework have similar constructors. You can use these constructors to create a table large enough to store all the elements you expect it to hold without requiring expensive resizing operations. In practice, however, they have proved confusing and difficult to use: confusing, because their int parameter, often misunderstood to be the expected number of entries, is in fact used to compute the table size, and difficult to use because computing the argument correctly from the expected maximum number of entries is implementation-dependent and error-prone. So Java 19 added static factory methods, which take only a parameter signifying the expected maximum number of entries. These methods are recommended as being easier to use and less subject to implementation changes. The factory method for HashSet is newHashSet:

static <T> HashSet<T> newHashSet(int numElements)

HashSet is unsychronized and not thread-safe; its iterators are fail-fast.

# CopyOnWriteArraySet

In functional terms, CopyOnWriteArraySet is another straightforward implementation of the Set contract, but with quite different performance characteristics from HashSet. This class is implemented as a thin wrapper around an instance of CopyOnWriteArrayList, which in turn is backed by an array. The array is treated as immutable; any modification of the set results in the creation of an entirely new array. So add has complexity $O ( n )$ , as does contains, which has to be implemented by a linear search. Clearly you wouldn’t use CopyOnWriteArraySet in a context where you were expecting many searches or insertions. But the array implementation means that iteration costs O(1) per element—faster than HashSet—and it has one advantage which is really compelling in some applications: it provides thread safety (see “Collections and Thread Safety”) without adding to the cost of read operations. This is in contrast to those collections which use locking to achieve thread safety for all operations (for example, the synchronized collections of “Synchronized Collections”). Locking operations are always a potential bottleneck in multi-threaded applications. By contrast, read operations on copy-on-write collections are implemented on the backing array, and thanks to its immutability they can be used by any thread without danger of interference from a concurrent write operation.

When would you want to use a set with these characteristics? In fairly specialized cases; one that is quite common is in the implementation of the Subject-Observer design pattern (see [Link to Come]), which requires events to be notified to a set of observers. This set may not be modified during the process of notification; with locking set implementations, read and write operations share the overhead necessary to ensure this, whereas with CopyOnWriteArraySet the overhead is carried entirely by write operations. This makes sense for Subject-Observer: in typical uses of this pattern, event notifications occur much more frequently than changes to the listener set.

Iterators for CopyOnWriteArraySet can be used only to read the set. When they are created, they are attached to the instance of the backing array being used by the set at that moment. Since no instance of this array is ever modified, the iterators’ remove method is not implemented. These are snapshot iterators (see “Collections and Thread Safety”); they reflect the state of the set at the time they were created, and can subsequently be traversed without any danger of interference from threads modifying the set from which they were derived.

Since there are no configuration parameters for CopyOnWriteArraySet, the constructors are just the standard ones discussed in “Collection Constructors”.

# EnumSet

This class exists to take advantage of the efficient implementations that are possible when the number of possible elements is fixed and a unique index can be assigned to each. These two conditions hold for a set of elements of the same Enum; the number of keys is fixed by the constants of the enumerated type, and the ordinal method returns values that are guaranteed to be unique to each constant. In addition, the values that ordinal returns form a compact range, starting from zero—ideal, in fact, for use as array indices or, in the standard implementation, indices of a bit vector. So add, remove, and contains are implemented as bit manipulations, with constant-time performance. Bit manipulation on a single word is extremely fast, and a long value can be used to represent EnumSets over enum types with up to 64 values. Larger enums can be treated in a similar way, with some overhead, using more than one word for the representation.

EnumSet is an abstract class that implements these different representations by means of different package-private subclasses. It hides the concrete implementation from the programmer, instead exposing factory methods that call the constructor for the appropriate subclass. The following group of static factory methods provide ways of creating EnumSets with

different initial contents: empty, specified elements only, or all elements of the enum.

$<E$ extends Enum $<E>>$ EnumSet $\leq E$ of(E first, E... rest) // create a set initially containing the specified elements $<E$ extends Enum $<E>>$ EnumSet $\leq E$ range(E from, E to) // create a set initially containing all of the elements in // the range defined by the two specified endpoints $<E$ extends Enum $<E>>$ EnumSet $\leq E$ allof(Class $\leq E$ elementType) // create a set initially containing all elements in elementType $<E$ extends Enum $<E>>$ EnumSet $\leq E$ noneOf(Class $\leq E$ elementType) // create a set of elementType, initially empty

An EnumSet contains the reified type of its elements, which is used at run time for checking the validity of new entries. This type is supplied by the above factory methods in two different ways. The methods of and range receive at least one enum argument, which can be queried for its declaring class (that is, the Enum that it belongs to). For allOf and noneOf, which have no enum arguments, a class token is supplied instead.

Common cases for EnumSet creation are optimized by the second group of methods, which allow you to efficiently create sets with one, two, three, four, or five elements of an enumerated type.

$<E$ extends Enum $<E>>$ EnumSet $<E>$ of(E e) $<E$ extends Enum $<E>>$ EnumSet $<E>$ of(E e1, E e2) $<E$ extends Enum $<E>>$ EnumSet $<E>$ of(E e1, E e2, E e3) $<E$ extends Enum $<E>>$ EnumSet $<E>$ of(E e1, E e2, E e3, E e4) $<E$ extends Enum $<E>>$ EnumSet $<E>$ of(E e1, E e2, E e3, E e4, E e5)

The third set of methods allows the creation of an EnumSet from an existing collection:

$<E$ extends Enum $<E>>$ EnumSet $<E>$ copyOf(EnumSet $<E>$ s) // create an EnumSet with the same element type as s, and // with the same elements $<E$ extends Enum $<E>>$ EnumSet $<E>$ copyOf(Collection $<E>$ c) // create an EnumSet from the elements of c, which must

```txt
contain
// at least one element
<E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s)
// create an EnumSet with the same element type as s,
// containing the elements not in s 
```

The collection supplied as the argument to the second version of copyOf must be nonempty so that the element type can be determined.

In use, EnumSet obeys the contract for Set, with the added requirement that its iterators will return their elements in their natural order (the order in which their enum constants are declared). It is not thread-safe, but unlike the unsynchronized general-purpose collections, its iterators are not failfast. They may be either snapshot or weakly consistent; to be conservative, the contract guarantees only that they will be weakly consistent (see “Collections and Thread Safety”).

# UnmodifiableSet

You won’t find any reference to the implementation(s)

UnmodifiableSet<E> of the Set interface in the Javadoc or in the code of the Collections Framework: it’s a name invented in this book for a family of package-private classes that client programmers can never access by name, but that are important because they provide the implementation of the unmodifiable sets obtained from the various overloads of the factory methods Set.of and Set.copyOf. These methods (which are static, like all factory methods—the keyword is omitted below for brevity) can be divided into three groups analogous to those of EnumSet. The first contains just an overload of the method of that takes no arguments and returns an empty unmodifiable set:

<E> Set<E> of() // Returns an unmodifiable list containing zero elements

The second group allows you to create an unmodifiable set from up to ten specified elements:

$<E>$ Set $<E>$ of(E e1) // Returns an unmodifiable set containing one element $<E>$ Set $<E>$ of(E e1, E e2) // Returns an unmodifiable set containing two elements $<E>$ Set $<E>$ of(E e1, E e2, E e3) // Returns an unmodifiable set containing three  
// elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4) // Returns an unmodifiable set containing four  
// elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4, E e5) // Returns an unmodifiable set containing  
// five elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4, E e5, E e6) // Returns an unmodifiable set  
// containing six  
elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) // Returns an unmodifiable  
// set containing seven  
elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) // Returns an  
// unmodifiable set  
containing eight elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) // Returns an  
// unmodifiable set  
containing nine elements $<E>$ Set $<E>$ of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) // Returns an  
// unmodifiable set  
containing ten elements

The third group allows you to create an unmodifiable set from a collection, an array, or a varargs-supplied list of arguments:

$<E>$ Set $<E>$ copyOf(Collection<? extends E> coll) // Returns an  
unmodifiable set containing // an arbitrary number  
of elements $<E>$ Set $<E>$ of(E... elements) // Returns an unmodifiable set

The classes that make up UnmodifiableSet take advantage of its unmodifiability to use fixed-length arrays as the backing structures. Without the overhead of empty table buckets or linked overflow structures, these implementations require much less space than a hashed structure. Iteration is also correspondingly more efficient, with the added benefit of improved spatial locality (see “Memory”). The tradeoff for faster iteration is that containment can only be determined by a linear search, $O ( n )$ in complexity.

In the OpenJDK implementation, the iterators for UnmodifiableSet have an unusual characteristic: the order of the iteration is randomly determined for each virtual machine instance. The reason for this design is to avoid replicating an odd sort of compatibility problem with HashSet: in the past, developers have based tests and even production code on the apparently fixed iteration order of HashSet. But since the contract for HashSet doesn’t specify the iteration order, implementers have changed it between versions, resulting in code breaking on upgrades. The iterators for unmodifiable sets have been designed to prevent developers being lulled into this error.

# SequencedSet

A SequencedSet is an externally or internally ordered Set that also exposes the methods of SequencedCollection. It combines the methods of these two interfaces, adding to them only in one respect: it provides a covariant override of the method reversed of SequencedCollection in order to return a value of type SequencedSet (see ???). It has a single direct implementation, LinkedHashSet, and is extended by the interface NavigableSet (actually by its parent interface SortedSet, but see the introductory remarks in “NavigableSet”).

![](images/ef85ccc190598ab5caa6ddf9d234ac21d633abfa76aaccbf32913a7100b0b839.jpg)  
Figure 8-2. SequencedSet and Related Types

# LinkedHashSet

This class inherits from HashSet and, further, implements SequencedSet by maintaining a linked list of its elements, as shown by the curved arrows in Figure 8-3. The situation in that figure would result from this code:

```javascript
var lhs = LinkedHashSet.<Character>newLinkedHashSet(3); Collections.addAll(lhs, 'j', 'i', 'b'); 
```

```txt
// iterators of a LinkedHashSet return their elements in insertion order: assert lhs.toString().equals("[j, i, b]") 
```

```txt
// the methods of SequencedSet are all available: assert lhs.getLast() == 'b'; assert lhs.reverse().ToString().equals("[b, i, j]") 
```

![](images/4215125c48e1f690eaae75b298c1dda23e09d5a823dba748a554884803bf631e.jpg)  
Figure 8-3. A linked hash table

The linked structure also has a useful consequence in terms of improved performance for iteration: next executes in constant time, as the linked list can be used to visit each element in turn. This is in contrast to HashSet, for which every bucket in the hash table must be visited whether it is

occupied or not. In the case of a large table that is sparsely occupied that would be a significant inefficiency, but in general the overhead involved in maintaining the linked list means that you would choose

LinkedHashSet in preference to HashSet only if that situation was likely to apply in your application.

The constructors for LinkedHashSet provide the same facilities as those of HashSet for configuring the underlying hash table. And in the same way as HashSet, it has a modern factory method newLinkedHashSet that accepts as its argument the expected number of elements and sizes the table optimally.

This class is unsychronized and not thread-safe; its iterators are fail-fast.

# NavigableSet

The interface NavigableSet adds to the SequencedSet contract a guarantee that its iterator will traverse the set in ascending element order, and adds further methods to find the elements adjacent to a target value. Prior to Java 6, when NavigableSet was introduced, the only subinterface of Set was an interface called SortedSet, which guarantees iteration order but does not expose the closest-match methods.

SortedSet is still in the JDK—it extends SequencedSet and is in its turn extended by NavigableSet—but is no longer of much interest, since it has no direct implementations in the platform.

We can use NavigableSet to add some useful functionality to the to-do manager. Until now, the methods of Collection and Set have provided no help in ordering our tasks—surely one of the central requirements of a to-do manager. Example 8-1 defines a record PriorityTask which attaches a priority to a task. There are three priorities, HIGH, MEDIUM, and LOW, declared so that HIGH priority comes first (i.e. is lowest) in the natural ordering. To compare two PriorityTasks, the comparator priorityTaskCmpr first compares their priorities: if the priorities are unequal, the higher priority tasks comes first. If the priorities are equal, it

goes on to use the natural ordering on the underlying tasks. So two objects can only compare as equal if they actually are equal—that is, the natural ordering is consistent with equals, and the problems discussed at the beginning of this Chapter don’t arise. Conversely, an example of how they could arise would be if we defined the comparator such that all tasks of equal priority compared as equal. With that definition, a NavigableSet could only contain at most one task of each priority.

Example 8-1. The record PriorityTask   
public enum Priority { HIGH, MEDIUM, LOW }   
public record PriorityTask(Task task, Priority priority) implements Comparable<PriorityTask> { static Comparator<PriorityTask>priorityTaskCmpr $=$ Comparator.comparing(PriorityTask::priority) .thenComparing(PriorityTask::task); public int compareTo(PriorityTask pt) { return priorityTaskCmpr(compare(this,pt); }

The following code shows NavigableSet working with a set of PriorityTasks.

```txt
NavigableSet<PriorityTask> priorityTasks = new TreeSet<PriorityTask>();  
priorityTasks.add(new PriorityTask(mikePhone, Priority.MEDIUM));  
priorityTasks.add(new PriorityTask(paulPhone, Priority.HIGH));  
priorityTasks.add(new PriorityTask(databaseCode, Priority.MEDIUM));  
priorityTasks.add(new PriorityTask(guiCode, Priority.LOW));  
assert(priorityTasks.toString().equals("");  
[PriorityTask[task=PhoneTask[name=Paul, number=123 4567], priority=HIGH], \]  
PriorityTask[task=CodingTask[spec=db], priority=MEDIUM], \]  
PriorityTask[task=PhoneTask[name=Mike, number=987 6543], priority=MEDIUM], \]  
PriorityTask[task=CodingTask[spec=gui], priority=LOW]](""); 
```

The methods defined by the NavigableSet interface fall into six groups:

# Retrieving the Comparator

Comparator<? super E> comparator()

This method returns the set’s comparator if it has been given one at construction time, or null if the set uses the natural ordering of its elements. The reason that the bounded type can be used for the Comparator parameter is because a NavigableSet parameterized on E can rely for ordering on a Comparator defined on any supertype of E. For example, recalling “A Fruity Example”, a Comparator<Fruit> could be used for a NavigableSet<Apple>.

# Inspecting the First and Last Elements

E first() // return the first element in the set E last() // return the last element in the set

If the set is empty, these operations throw NoSuchElementException.

# Removing the First and Last Elements

E pollFirst() // retrieve and remove the first (lowest) element, // or return null if this set is empty E pollLast() // retrieve and remove the last (highest) element, // or return null if this set is empty

These are analogous to the methods of the same name in Deque (see “Deques”), and help to support the use of NavigableSet in applications which require queue functionality. For example, in the version of the to-do manager in this section, we could get the highest-priority task off the list, ready to be carried out, by means of this:

PriorityTask nextTask $=$ priorityTasks.pollFirst(); assert nextTask.toString().equals( "PriorityTask[task=PhoneTask[name=Paul, number=123 4567], priority=HIGH]");

# Getting Range Views

An interval such as a range view can be open, half-open, or closed, depending on how many of its limit points it contains. For example, the range of numbers x for which $0 { \leq } \mathbf { X } { \leq } 1$ is closed, because it contains both limit points 0 and 1. The ranges $0 { \leq } \mathbf { X } { < } 1$ and $0 { < } \mathbf { x } { \leq } 1$ are half-open because they contain only one of the limit points, and the range $0 { < } \mathbf { x } { < } 1$ is open because it contains neither.

This preliminary explanation is necessary because each of the methods in this group appears in two overloads, one inherited from SortedSet and returning a half-open SortedSet view, and one defined in NavigableSet returning a NavigableSet view that can be open, half-open, or closed according to the user’s choice.

The SortedSet methods return a view of the set elements starting from the fromValue—including it if it is present—and up to but excluding the toValue. Notice that although the Javadoc for these methods calls the arguments to these operations “elements”—fromElement and toElement—this is misleading: in fact they do not themselves have to be members of the set.

```txt
SortedSet<E> subSet(E fromValue, E toValue)  
SortedSet<E> headSet(E toValue)  
SortedSet<E> tailSet(E fromValue) 
```

The NavigableSet methods are similar, but allow you to specify for each bound whether it should be included or excluded.

NavigableSet $\mathbf{\bar{E}}>$ subSet(E fromValue,boolean fromInclusive, E toValue, boolean toInclusive)   
NavigableSet $\mathbf{\bar{E}}>$ headset(E toValue,boolean inclusive)   
NavigableSet $\mathbf{\bar{E}}>$ tailSet(E fromValue,boolean inclusive)

In our example, these methods could be useful in providing different views of the elements in priorityTasks. For instance, we can use headSet to obtain a view of the high- and medium-priority tasks. To do this, we need

a special task that comes before all others in the task ordering; fortunately, we defined a class EmptyTask for just this purpose in “Using the Methods of Collection”. Using this, it’s easy to extract all tasks that come before any low-priority task:

```javascript
PriorityTask firstLowPriorityTask = new PriorityTask(new EmptyTask(), Priority.LOW);  
NavigableSet<PriorityTask> highAndMediumPriorityTasks = priorityTasks.headset(firstLowPriorityTask, false);  
assert(highAndMediumPriorityTasks.toString().equals("");[PriorityTask[task=PhoneTask[name=Paul, number=123 4567], priority=HIGH], \PriorityTask[task=CodingTask[spec=db], priority=MEDIUM], \PriorityTask[task=PhoneTask[name=Mike, number=987 6543], priority=MEDIUM]](""); 
```

In fact, because we know that tasks with empty details will never normally occur, we can also use one as the first endpoint in a half-open interval:

```javascript
PriorityTask firstMediumPriorityTask = new PriorityTask(new EmptyTask(), Priority.MEDIUM);  
NavigableSet<PriorityTask> mediumPriorityTasks = priorityTasks.set(firstMediumPriorityTask, firstLowPriorityTask, false);  
assert(mediumPriorityTasks.toString().equals("");[PriorityTask[task=CodingTask[spec=db], priority=MEDIUM], \PriorityTask[task=PhoneTask[name=Mike, number=987 6543],priority=MEDIUM]](""); 
```

These views are completely “transparent”; all changes in the underlying set are reflected in the view:

```txt
PriorityTask_logicCodeMedium = new PriorityTask(logicCode, Priority.MEDIUM);
priorityTasks.add(logicCodeMedium);
assert(mediumPriorityTasks.toString()).equals("");[PriorityTask[task=CodingTask[spec=db], priority=MEDIUM], \PriorityTask[task=CodingTask[spec=logic], priority=MEDIUM],\ 
```

PriorityTask[task $=$ PhoneTask[name=Mike, number=987 6543], priority=MEDIUM]]""");

To understand how this works, think of all the possible values in an ordering as lying on a line, like the number line used in arithmetic. A range is defined as a fixed segment of that line, regardless of which values are actually in the original set. So a subset, defined on a NavigableSet and a range, will allow you to work with whichever elements of the NavigableSet currently lie within the range.

The converse also applies: changes in the view—including structural changes—are reflected in the underlying set:

```txt
assert priorityTasks.size() == 5; mediumPriorityTasks.remove(logicCodeMedium); assert priorityTasks.size() == 4; 
```

# Getting Closest Matches

```javascript
E ceiling(E e) // return the least element x in this set such that // x≥e, or null if there is no such element  
E floor(E e) // return the greatest element x in this set such that // x≤e, or null if there is no such element  
E higher(E e) // return the least element x in this set such that // x>e, or null if there is no such element  
E lower(E e) // return the greatest element in this set such that // x<e, or null if there is no such element 
```

These methods are useful for short-distance navigation. For example, suppose that we want to find, in a sorted set of strings, the last three strings in the subset that is bounded above by “x-ray”, including that string itself if it is present in the set. NavigableSet methods make this easy:

```java
NavigableSet<String> stringSet = new TreeSet<>(); Collections.addAll(stringSet, "abc", "cde", "x-ray", "zed")); 
```

```cpp
Optional<String> last =   
Optional.ofNullable(stringSet.floor("x-ray")); assert last.equals(Optional.of("x-ray"));   
Optional<String> secondToLast =   
last.map(stringSet:::lower); assert secondToLast.equals(Optional.of("cde")); Optional<String> thirdToLast =   
secondToLast.map(stringSet:::lower); assert thirdToLast.equals(Optional.of("abc")); 
```

Notice that in line with a general trend in the design of the Collections Framework, NavigableSet returns null values to signify the absence of elements where, for example, the first and last methods of SortedSet would throw NoSuchElementException. For this reason, you should avoid null elements in NavigableSets, and in fact the newer implementation, ConcurrentSkipListSet, does not permit them (though TreeSet must continue to do so, for backward compatibility).

# Navigating the Set in Reverse Order

```txt
NavigableSet<E> descendingSet() // return a reverse-order view of  
// the elements in this set  
Iterator<E> descendingIterator() // return a reverse-order iterator 
```

Methods of this group make traversing a NavigableSet equally easy in the descending (that is, reverse) ordering. As a simple illustration, let’s generalise the example above using the nearest-match methods. Suppose that, instead of finding just the last three strings in the sorted set bounded above by “x-ray”, we want to iterate over all the strings in that set, in descending order:

```groovy
NavigableSet<String> headSet = stringSet.headSet(last, true);  
NavigableSet<String> reverseHeadSet = headSetdescendingSet();  
assert reverseHeadSet.toString().equals("[x-ray, cde, abc]")  
String conc = " ";  
for (String s : reverseHeadSet) {  
    conc += s + " ";  
} 
```

```typescript
} assert conc.equals(" x-ray cde abc "); 
```

If the iterative processing involves structural changes to the set, and the implementation being used is TreeSet (which has fail-fast iterators), we will have to use an explicit iterator to avoid ConcurrentModificationException:

```txt
for (Iterator<String> itr = headSet descendingIterator();  
itr.hasNext();）{  
    itr.next(); itr.remove();  
}  
assert headSet.isEmpty(); 
```

Although it has been retrofitted to extend SequencedSet, none of the new methods provide any different functionality: they are just renamed versions of existing methods. Table 8-1 shows the correspondence between the new and existing methods.

Table 8-1. SequencedCollection methods, with their NavigableSe t equivalents   

<table><tr><td>SequencedCollectionion</td><td>NavigableSet</td></tr><tr><td>getFirst</td><td>first (inherited from SortedSet)</td></tr><tr><td>getLast</td><td>last (inherited from SortedSet)</td></tr><tr><td>removeFirst</td><td>pollFirst</td></tr><tr><td>removeLast</td><td>pollLast</td></tr><tr><td>addFirst</td><td>unsupported method for internally ordered collections</td></tr><tr><td>addLast</td><td>unsupported method for internally ordered collections</td></tr><tr><td>reversed</td><td>descendingSet</td></tr></table>

# TreeSet

This is the first tree implementation that we have seen, so we should take a little time now to consider how trees perform in comparison to the other implementation types used by the Collections Framework.

Trees are the data structure you would choose for an application that needs fast insertion and retrieval of individual elements but which also requires that they be held in sorted order.

For example, suppose you want to match all the words from a set against a given prefix, a common requirement in visual applications where a dropdown should ideally show all the possible elements that match against the prefix that the user has typed. A hash table can’t return its elements in sorted order and a list can’t retrieve its elements quickly by their content, but a tree can do both.

In computing, a tree is a branching structure that represents hierarchy. Computing trees borrow a lot of their terminology from genealogical trees, though there are some differences; the most important is that, in computing trees, each node has only one parent—except the root, which has none. An important class of tree often used in computing is a binary tree—one in which each node can have at most two children. Figure 8-4 shows an example of a binary tree containing the words of this sentence in alphabetical order.

![](images/f41d5d4bdb0e77a1c84f7aa10b0d980aa83c3d3c0848abc7932488f3f21932c7.jpg)  
Figure 8-4. An ordered, balanced binary tree

The most important property of this tree can be seen if you look at any nonleaf node—say, the one containing the word the: all the nodes below that on the left contain words that precede the alphabetically, and all those on the right, words that follow it. To locate a word, you would start at the root and descend level by level, doing an alphabetic comparison at each level, so the cost of retrieving or inserting an element is proportional to the depth of the tree.

How deep, then, is a tree that contains n elements? The complete binary tree with two levels has three elements (that’s $2 ^ { 2 } { - } 1 { \dot { } }$ ), and the one with three levels has seven elements $( 2 ^ { 3 } – 1 )$ . In general, a binary tree with n complete levels will have $2 ^ { n } { - } 1$ elements, and the depth of a tree with n elements will be bounded by log $n$ (since $2 \land \log n ^ { \land } = n )$ . Just as n grows much more slowly than $2 ^ { n }$ , log $n$ grows much more slowly than n. So contains on a large tree is much faster than on a list containing the same elements. It’s still not as good as on a hash table—whose operations can ideally work in constant time—but a tree has the big advantage over a hash table that its iterator can return its elements in sorted order.

Not all binary trees will have this nice performance, though. Figure 8-4 shows a balanced binary tree—one in which each node has an equal number of descendants (or as near as possible) on each side. An unbalanced tree can give much worse performance—in the worst case, as bad as a linked list (see Figure 8-5). TreeSet uses a data type called a red-black tree, which has the advantage that if it becomes unbalanced through insertion or removal of an element, it can always be rebalanced in $O ( \log n )$ time.

![](images/2e880279a1e7fb01d7df1ad92508e21195c8448a957131c4be5dcde08cab2f44.jpg)  
Figure 8-5. An unbalanced binary tree

The constructors for TreeSet include, besides the standard ones, one which allows you to supply a Comparator (see “Comparator<T>”) and one which allows you to create one from another SortedSet:

```typescript
TreeSet(Comparator<? super E> c) // construct an empty set which will be sorted using the // specified comparator TreeSet(SortedSet<E> s) // construct a new set containing the elements of the // supplied set, sorted according to the same ordering 
```

The second of these is rather too close in its declaration to the standard “conversion constructor” (see “Collection Constructors”):

```txt
TreeSet(Collection<? extends E> c) // construct a new set containing the elements of the // supplied set, sorted according to the natural ordering 
```

As Joshua Bloch explains in Effective Java (item 52 in the third edition, “Use overloading judiciously”), calling one of two constructor or method overloads which take parameters of related type can give confusing results. This is because, in Java, calls to overloaded constructors and methods are resolved at compile time on the basis of the static type of the argument, so applying a cast to an argument can make a big difference to the result of the call, as the following code shows:

```dart
// construct and populate a NavigableSet whose iterator returns its  
// elements in the reverse of natural order:  
NavigableSet<String> base = new TreeSet<>  
(Comparator.reverseOrder());  
Collections.addAll(base, "b", "a", "c"));  
// call the two different constructors for TreeSet, supplying the  
// set just constructed, but with different static types:  
NavigableSet<String> sortedSet1 = new TreeSet<>  
((Set<String>)base);  
NavigableSet<String> sortedSet2 = new TreeSet<> (base);  
// and the two sets have different iteration orders:  
List<String> forward = new ArrayList<> (sortedSet1);  
List<String> backward = new ArrayList<> (sortedSet2); 
```

assert !forward.equals(backward); assert forward.reversed().equals(backward);

This problem afflicts the constructors for all the sorted collections in the Framework (TreeSet, TreeMap, ConcurrentSkipListSet, and ConcurrentSkipListMap). To avoid it in your own class designs, choose parameter types for different overloads so that an argument of a type appropriate to one overload cannot be cast to the type appropriate to a different one. If that is not possible, the two overloads should be designed to behave identically with the same argument, regardless of its static type. For example, a PriorityQueue (“PriorityQueue”) constructed from a collection uses the ordering of the original, whether the static type with which the constructor is supplied is one of the Comparator-containing types PriorityQueue or SortedSet, or just a plain Collection. To achieve this, the conversion constructor uses a instanceof tests to determine the type of the supplied collection, and if that is a SortedSet or a PriorityQueue, it extracts the Comparator, only falling back on natural ordering if there isn’t one.

TreeSet is unsychronized and not thread-safe; its iterators are fail-fast.

# ConcurrentSkipListSet

ConcurrentSkipListSet was introduced in Java 6 as the first concurrent set implementation. It is backed by a skip list, a modern alternative to the binary trees of the previous section. A skip list for a set is a series of linked lists, each of which is a chain of cells consisting of two fields: one to hold a value, and one to hold a reference to the next cell. Elements are inserted into and removed from a linked list in constant time by pointer rearrangement, as shown in Figure 8-6, parts (a) and (b) respectively.

![](images/c31e9d7a0ce3dc05e2d09f6a1427dabd6de575c0fedbbc055360f2d4fef981d3.jpg)  
Figure 8-6. Modifying a linked list

![](images/2bff43c0ffe9a7068937aa81274a94f2ac57343c22bfbf0cab2cce34792ef65b.jpg)  
Figure 8-7. Searching a skip list   
Figure 8-7 shows a skip list consisting of three linked lists, labelled levels 0, 1 and 2. The first linked list of the collection (level 0 in the figure) contains the elements of the set, sorted according to their natural order or by the comparator of the set. Each list above level 0 contains a subset of the list below, chosen randomly according to a fixed probability. For this example, let’s suppose that the probability is 0.5; on average, each list will contain half the elements of the list below it. Navigating between links takes a fixed time, so the quickest way to find an element is to start at the beginning (the left-hand end) of the top list and to go as far as possible on each list before dropping to the one below it.

The curved arrows of Figure 8-7 shows the progress of a search for the element 55. The search starts with the element 12 at the top left of level 2, steps to the element 31 on that level, then finds that the next element is 61, higher than the search value. So it drops one level, and then repeats the process; element 47 is still smaller than 55, but 61 is again too large, so it once more drops a level and finds the search value in one further step.

Inserting an element into a skip list always involves at least inserting it at level 0. When that has been done, should it also be inserted at level 1? If level 1 contains, on average, half of the elements at level 0, then we should toss a coin (that is, randomly choose with probability 0.5) to decide whether it should be inserted at level 1 as well. If the coin toss does result in it being inserted at level 1, then the process is repeated for level 2, and so on. To remove an element from the skip list, it is removed from each level in which it occurs.

If the coin tossing goes badly, we could end up with every list above level 0 empty—or full, which would be just as bad. These outcomes have very low probability, however, and analysis shows that, in fact, the probability is very high that skip lists will give performance comparable to binary trees: search, insertion and removal all take $O ( \log n )$ . Their compelling advantage for concurrent use is that they have efficient lock-free insertion and deletion algorithms, whereas there are none known for binary trees.

The iterators of ConcurrentSkipListSet are weakly consistent.

Table 8-2. Comparative performance of different Set implementations   

<table><tr><td></td><td>add</td><td>contains</td><td>next</td><td>note</td></tr><tr><td>HashSet</td><td>O(1)</td><td>O(1)</td><td>O(h/n)</td><td>h is the capacity</td></tr><tr><td>LinkedHashSet</td><td>O(1)</td><td>O(1)</td><td>O(1)</td><td></td></tr><tr><td>CopyOnWriteArraySet</td><td>O(n)</td><td>O(n)</td><td>O(1)</td><td></td></tr><tr><td>EnumSet</td><td>O(1)</td><td>O(1)</td><td>O(1)*</td><td></td></tr><tr><td>TreeSet</td><td>O(log n)</td><td>O(log n)</td><td>O(log n)</td><td></td></tr><tr><td>ConcurrentSkipListSet</td><td>O(log n)</td><td>O(log n)</td><td>O(1)</td><td></td></tr></table>

*In the EnumSet implementation for enum types with more than 64 values, next has worst case complexity of $O ( \log m )$ , where m is the number of elements in the enumeration.

# Comparing Set Implementations

Table 8-2 shows the comparative performance of the different Set implementations. When you are choosing an implementation, of course, efficiency is only one of the factors you should take into account. Some of these implementations are specialized for specific situations; for example, EnumSet should always (and only) be used to represent sets of enum. Similarly, CopyOnWriteArraySet should only be used where set size

will remain relatively small, read operations greatly outnumber writes, thread safety is required, and read-only iterators are acceptable.

That leaves the general-purpose implementations: HashSet, LinkedHashSet, TreeSet, and ConcurrentSkipListSet. The first three are not thread-safe, so can only be used in multi-threaded code either in conjunction with client-side locking, or wrapped in Collection.synchronizedSet (see “Synchronized Collections”). For single-threaded applications where there is no requirement for the set to be sorted, your choice is between HashSet and LinkedHashSet. If your application will be frequently iterating over the set, or if you require access ordering, LinkedHashSet is the implementation of choice.

Finally, if you require the set to sort its elements, the choice is between TreeSet and ConcurrentSkipListSet. In a multi-threaded environment, ConcurrentSkipListSet is the only sensible choice. Even in single-threaded code ConcurrentSkipListSet may not show a significantly worse performance for small set sizes. For larger sets, however, or for applications in which there are frequent element deletions, TreeSet will perform better if your application doesn’t require thread safety.

OceanofPDF.com

# A NOTE FOR EARLY RELEASE READERS

With Early Release ebooks, you get books in their earliest form—the author’s raw and unedited content as they write—so you can take advantage of these technologies long before the official release of these titles.

This will be the 15th chapter of the final book. Please note that the GitHub repo will be made active later on.

If you have comments about how we might improve the content and/or examples in this book, or if you notice missing material within this chapter, please reach out to the editor at shunter@oreilly.com.

A queue is a collection designed to hold elements for processing, yielding them up in the order in which they are to be processed. The corresponding Collections Framework interface Queue<E> has a number of different implementations embodying different rules about what this order should be. Many of the implementations use the rule that tasks are to be processed in the order in which they were submitted (First In First Out, or FIFO), but other rules are possible—for example, the Collections Framework includes queue classes whose processing order is based on task priority. The Queue interface was introduced in Java 5, motivated in part by the need for queues in the concurrency utilities included in that release. A glance at the hierarchy of implementations shown in Figure 9-1 shows that, in fact, nearly all the Queue implementations in the Collections Framework are in the package java.util.concurrent.

One classic requirement for queues in concurrent systems arises when a number of tasks have to be executed by a number of threads working in parallel. An everyday example of this situation is that of a single queue of

airline passengers being handled by a line of check-in operators. Each operator works on processing a single passenger (or a group of passengers) while the remaining passengers wait in the queue. As they arrive, passengers join the tail of the queue, wait until they reach its head, and are then assigned to the next operator who becomes free. A good deal of fine detail is involved in implementing a queue such as this; operators have to be prevented from simultaneously attempting to process the same passenger, empty queues have to be handled correctly, and in computer systems there has to be a way of defining queues with a maximum size, or bound. (This last requirement may not often be imposed in airline terminals, but it can be very useful in systems in which there is a maximum waiting time for a task to be executed.) The Queue implementations in java.util.concurrent look after these implementation details for you.

![](images/61da6e178a267f70c13d6ec905d093a1da470dfb64c6b764c146e5c26b6672e8.jpg)  
Figure 9-1. Implementations of Queue in the Collections Framework

In addition to the operations inherited from Collection, the Queue interface includes operations to add an element to the tail of the queue, to inspect the element at its head, and to remove the element at its head. Each of these three operations comes in two varieties, one which returns a value to indicate failure and one which throws an exception.

# Adding an Element to a Queue

The exception-throwing variant of this operation is the add method inherited from Collection. Although add does return a boolean signifying its success in inserting an element, that value can’t be used to

report that a bounded queue is full; the contract for add specifies that it may return false only if the reason for refusing the element is that it was already present—otherwise, it must throw an (unchecked) exception. For a bounded queue, the value-returning variant offer is usually a better option:

boolean offer (E e) // insert the given element if possible

The value returned by offer indicates whether the element was successfully inserted. Note that offer does throw an exception if the element is illegal in some way (for example, the value null submitted to a queue that doesn’t permit nulls). Normally, if offer returns false, it has been called on a bounded queue that has reached capacity.

# Retrieving an Element from a Queue

The methods in this group are peek and element for inspecting the head element, and poll and remove for removing it from the queue and returning its value.

The methods that throw an exception for an empty queue are:

E element() // retrieve but do not remove the head element   
E remove() // retrieve and remove the head element

Notice that this is a different method from the Collection method remove(Object). The methods that return null for an empty queue are:

E peek() // retrieve but do not remove the head element   
E poll() // retrieve and remove the head element

Because these methods return null to signify that the queue is empty, you should avoid using null as a queue element. In general, the use of null as a queue element is discouraged by the Queue interface; in the JDK, the only implementation that allows it is the legacy class LinkedList.

# Using the Methods of Queue

Let’s look at examples of the use of these methods. Queues should provide a good way of implementing a task manager, since their main purpose is to yield up elements, such as tasks, for processing. For the moment we’ll use ArrayDeque as the fastest and most straightforward implementation of Queue (and also of Deque, of course). But, as before, we’ll confine ourselves to the methods of the interface—but remember that, in choosing a queue implementation, you’re also choosing the ordering of task processing. With ArrayDeque, you get FIFO ordering—very possibly the best choice for the task manager: since our attempts to get organized using fancy scheduling methods never seem to work very well, let’s try something simpler.

ArrayDeque is unbounded, so we could use either add or offer to set up the queue with new tasks.

```txt
Queue<Task> taskQueue = new ArrayListDeque<>();  
taskQueue.add(mikePhone);  
taskQueue.add(paulPhone); 
```

Any time we feel ready to do a task, we can take the one that has reached the head of the queue:

Task nextTask $=$ taskQueue poll(); if (nextTask $! =$ null）{ //process nextTask

The choice between using poll and remove depends on whether we want to regard queue emptiness as an exceptional condition. Realistically—given the nature of the application—that might be a sensible assumption, so this is an alternative:

```javascript
try { Task nextTask = taskQueue.remove(); // process nextTask } catch (NoSuchElementException e) { 
```

```scss
// but we \*never\* run out of tasks! } 
```

This scheme needs some refinement to allow for the nature of different kinds of tasks. Phone tasks fit into relatively short time slots, whereas we don’t like to start coding unless there is reasonably substantial time to get into the task. So if time is limited—say, until the next meeting—we might like to check that the next task is of the right kind before we take it off the queue:

Task nextTask $=$ taskQueuepeek();   
if (nextTask instanceof PhoneTask){ taskQueue.remove(); //process nextTask

These inspection and removal methods are a major benefit of the Queue interface; Collection has nothing like them (though NavigableSet does). The price we pay for this benefit is that the methods of Queue are useful to us only if the head element is actually one that we want. True, the class PriorityQueue allows us to provide a comparator that will order the queue elements so that the one you want is at the head, but that may not be a particularly good way of expressing the algorithm for choosing the next task—for example, you might need to know something about all the outstanding tasks before you can choose the next one. So in this situation, if our to-do manager is entirely queue-based, you may end up going for coffee until the meeting starts. As an alternative, you could consider using the List interface, which provides more flexible means of accessing its elements but has the drawback that its implementations provide much less support for multi-thread use.

This might sound overly pessimistic; after all, Queue is a subinterface of Collection, so it inherits methods that support traversal, like iterator. In fact, although these methods are implemented, their use is not recommended in normal situations. In the design of the queue classes, efficiency in traversal has been traded against fast implementation of the

methods of Queue; in addition, queue iterators do not guarantee to return their elements in proper sequence and, for some concurrent queues, will actually fail in normal conditions (see “BlockingQueue Implementations”).

In the next section we shall look at the two direct JDK implementations of Queue—PriorityQueue and ConcurrentLinkedList—and, in “BlockingQueue”, at BlockingQueue and its implementations. The classes in these two sections differ widely in their behavior. Most of them are thread-safe; most provide blocking facilities (that is, operations that wait for conditions to be right for them to execute); some support priority ordering; one—DelayQueue—holds elements until their delay has expired, and another—SynchronousQueue—is purely a synchronization facility. In choosing between Queue implementations, you would be influenced more by these functional differences than by their performances.

# Queue Implementations

# PriorityQueue

PriorityQueue is one of the two non-legacy Queue implementations (that is, other than LinkedList) not designed primarily for concurrent use (the other one is ArrayDeque). It is not thread-safe, nor does it provide blocking behavior. It gives up its elements for processing according to an ordering like that used by NavigableSet—either the natural order of its elements if they implement Comparable, or the ordering imposed by a Comparator supplied when the PriorityQueue is constructed. So PriorityQueue would be an alternative design choice (obviously, given its name) for the priority-based to-do manager that we outlined in “NavigableSet” using NavigableSet. Your application will dictate which alternative to choose: if it needs to examine and manipulate the set of waiting tasks, use NavigableSet. If its main requirement is efficient access to the next task to be performed, use PriorityQueue.

Choosing PriorityQueue allows us to reconsider the ordering: since it accommodates duplicates, it does not share the requirement of NavigableSet for an ordering consistent with equals. To emphasize the point, we will define a new ordering for our to-do manager that depends only on priorities. Contrary to what you might expect, PriorityQueue gives no guarantee of how it presents multiple elements with the same value. So if, in our example, several tasks are tied for the highest priority in the queue, it will choose one of them arbitrarily as the head element.

The constructors for PriorityQueue are:   
```txt
PriorityQueue() // natural ordering, default initial capacity (11)  
PriorityQueue(Collection<? extends E> c) // natural ordering of elements taken from c, unless // c is a PriorityQueue or SortedSet, in which case // copy c's ordering  
PriorityQueue(int initialCapacity) // natural ordering, specified initial capacity  
PriorityQueue(int initialCapacity, Comparator<? super E> comparator) // Comparator ordering, specified initial capacity  
PriorityQueue(PriorityQueue<? extends E> c) // ordering and elements copied from c  
PriorityQueue(SortedSet<? extends E> c) // ordering and elements copied from c 
```

Notice how the second of these constructors avoids the problem of the overloaded TreeSet constructor that we discussed in “TreeSet”. We can use PriorityQueue for a simple implementation of our to-do manager with the PriorityTask class defined in “NavigableSet”, and a new Comparator depending only on the task’s priority:

```cpp
final int INITIAL_CAPACITY = 10;  
Comparator<PriorityTask> priorityComp = Comparator.comparing(PriorityTask::priority);  
Queue<PriorityTask> priorityQueue = new PriorityQueue<> 
```

```matlab
(INITIAL_CAPACITY, priorityComp);  
priorityQueue.add(new PriorityTask(mikePhone, Priority.MEDIUM));  
priorityQueue.add(new PriorityTask(paulPhone, Priority.HIGH));  
PriorityTask nextTask = priorityQueue poll();  
System.out.println(nextTask);  
...  
PriorityTask nextTask = priorityQueue POLL(); 
```

Priority queues are usually efficiently implemented by priority heaps. A priority heap is a binary tree somewhat like those we saw implementing TreeSet in “TreeSet”, but with two differences: first, the only ordering constraint is that each node in the tree should be greater than either of its children, and second, that the tree should be complete at every level except possibly the lowest; if the lowest level is incomplete, the nodes it contains must be grouped together at the left. Figure 9-2(a) shows a small priority heap, with each node shown only by the field containing its priority. To add a new element to a priority heap, it is first attached at the leftmost vacant position, as shown by the circled node in Figure 9-2(b). Then it is repeatedly exchanged with its parent until it reaches a parent that has higher priority. In the figure, this required only a single exchange of the new element with its parent, giving Figure 9-2(c). (Nodes shown circled in Figures Figure 9-2 and Figure 9-3 have just changed position.)

![](images/5e262d4fbda834aedc4e97361a9900c14692802e555be2782593364b499ea816.jpg)

![](images/4218ccb6726c79f1f60a21ac8fc7d968b3ca4adf8a8d8bbafac766927c7c064e.jpg)

![](images/a2747d7aee17594982932b2cc75aace7d6281187a63a40f9e5b79362321b57ad.jpg)  
Figure 9-2. Adding an element to a PriorityQueue

Getting the highest-priority element from a priority heap is trivial: it is the root of the tree. But, when that has been removed, the two separate trees that result must be reorganized into a priority heap again. This is done by first placing the rightmost element from the bottom row into the root

position. Then—in the reverse of the procedure for adding an element—it is repeatedly exchanged with the larger of its children until it has a higher priority than either. Figure 9-3 shows the process—again requiring only a single exchange—starting from the heap in Figure 9-2(c) after the head has been removed.

Apart from constant overheads, both addition and removal of elements require a number of operations proportional to the height of the tree. So PriorityQueue provides $O ( \log n )$ time for offer, poll, remove(), and add. The methods remove(Object) and contains may require the entire tree to be traversed, so they require $O ( n )$ time. The methods peek and element, which just retrieve the root of the tree without removing it, take constant time, as does size, which uses an object field that is continually updated.

![](images/dc474904e7af7c92b1a953e2e4088f69201ae38e8923c2a9a88e67d0c6b347d6.jpg)

![](images/060a5264b25700ff12994de7890b0c3cac8f966a8cd1c19b8bbcc76681ff8fc0.jpg)

![](images/4358c689f20638d475fac4fbf49386e242363ab56b22b7c9ec7806e99eca270d.jpg)  
Figure 9-3. Removing the head of a PriorityQueue

PriorityQueue is not suitable for concurrent use. Its iterators are failfast, and it doesn’t offer support for client-side locking. A thread-safe version, PriorityBlockingQueue (see “BlockingQueue Implementations”), is provided instead.

# ConcurrentLinkedQueue

The other nonblocking Queue implementation is ConcurrentLinkedQueue, an unbounded, thread-safe, FIFO-ordered queue. It uses a linked structure, similar to those we saw in “ConcurrentSkipListSet” as the basis for skip lists, and in “HashSet” for

hash table overflow chaining. We noticed there that one of the main attractions