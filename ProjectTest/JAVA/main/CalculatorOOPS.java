// CalculatorOOPS/Divide.java

package projecteval.CalculatorOOPS;

public class Divide implements Operate {
    @Override
    public Double getResult(Double... numbers){
        Double div = numbers[0];

        for(int i=1;i< numbers.length;i++){
            div /= numbers[i];
        }
        return div;
    }
}


// CalculatorOOPS/Modulus.java

package projecteval.CalculatorOOPS;

public class Modulus implements Operate{
    @Override
    public Double getResult(Double... numbers){
        Double mod = numbers[0];

        for (int i = 1; i < numbers.length; i++) {
            mod %= numbers[i];
        }
        return mod;
    }
}


// CalculatorOOPS/ReadInput.java

package projecteval.CalculatorOOPS;

import java.util.Scanner;

public class ReadInput {
    public static String read(){
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Input Expression Example: 4*3/2");
        String inputLine = scanner.nextLine();
        
        scanner.close();
        return inputLine;
    }
}

// CalculatorOOPS/Operate.java

package projecteval.CalculatorOOPS;

public interface Operate {
    Double getResult(Double... numbers);
}


// CalculatorOOPS/Add.java

package projecteval.CalculatorOOPS;

public class Add implements Operate{
    @Override
    public Double getResult(Double... numbers){
        Double sum = 0.0;

        for(Double num: numbers){
            sum += num;
        }
        return sum;
    }
}


// CalculatorOOPS/Sub.java

package projecteval.CalculatorOOPS;

public class Sub implements Operate{
    @Override
    public Double getResult(Double... numbers){
        Double sub = numbers[0];

        for(int i=1;i< numbers.length;i++){
            sub -= numbers[i];
        }
        return sub;
    }
}


// CalculatorOOPS/Multiply.java

package projecteval.CalculatorOOPS;

public class Multiply implements Operate {
    @Override
    public Double getResult(Double... numbers){
        Double mul = 1.0;

        for(Double num: numbers){
            mul *= num;
        }
        return mul;
    }

}


// CalculatorOOPS/Calculator.java

package projecteval.CalculatorOOPS;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class Calculator {
    public static void main(String[] args){
        final String inputExp = ReadInput.read();

        Queue<String> operations;
        Queue<String> numbers;

        String[] numbersArr = inputExp.split("[-+*/%]");
//        String[] operArr = inputExp.split("[0-9]+");
        String[] operArr = inputExp.split("\\d+");
        numbers = new LinkedList<>(Arrays.asList(numbersArr));
        operations = new LinkedList<>(Arrays.asList(operArr));

        Double res = Double.parseDouble(Objects.requireNonNull(numbers.poll()));

        while(!numbers.isEmpty()){
            String opr = operations.poll();

            Operate operate;
            switch(Objects.requireNonNull(opr)){
                case "+":
                    operate = new Add();
                    break;
                case "-":
                    operate = new Sub();
                    break;
                case "*":
                    operate = new Multiply();
                    break;
                case "/":
                    operate = new Divide();
                    break;
                case "%":
                    operate = new Modulus();
                    break;
                default:
                    continue;
            }
            Double num = Double.parseDouble(Objects.requireNonNull(numbers.poll()));
            res = operate.getResult(res, num);
        }

        System.out.println(res);
    }
}

