import java.util.Formatter;
import java.lang.SecurityException;
import java.util.FormatterClosedException;
import java.io.FileNotFoundException;
import java.lang.Math;
import java.io.File;

public class CreateDataSet {

    public static void main(String[] args) {
            if (args.length < 2){
		    System.out.println("Uso: <nome-programa> <tamanho-arquivo> <unidade: K|M>");
	            System.exit(1);
            }

            long size = Long.parseLong(args[0]);
	    String unit = args[1];
          
	    if (unit.equalsIgnoreCase("K"))
	        size = size * 1000;
	    else	
	    	size = size * 1000000;
	
	    String[] string = {"Sand e Kleber sao de Goaiania ", "Reinaldo e Karina sao de Fortaleza ", "e a o "};
            int[] counter = {0,0,0};

	    String fileName = "teste" + args[0] + args[1] + ".txt"; 

	    Formatter output = null;
	    File fileInfo = null;

	    try{
	           output = new Formatter(fileName);
		   fileInfo = new File(fileName);
	    } catch (SecurityException e){
		   System.out.println("Erro ao tentar abrir arquivo");
		   System.exit(1);
	    } catch (FileNotFoundException e){
                System.out.println("Erro ao tentar abrir arquivo");
		   System.exit(1);
	    }

            while (fileInfo.length() < size) 
            {
		   int index = ((int)(Math.random() * 10)) % 3;
		   
		   try {
		          output.format("%s",  string[index]);
			  output.flush();
	                  counter[index] = counter[index] + 1;
		   } catch (FormatterClosedException e){
			   System.out.println("Erro ao tentar gravar arquivo");
		           System.exit(1);
		   }

	    }

	    for (int i = 0; i < 3; i++)
		   System.out.println("String: " + string[i] + "Count: " + counter[i]);
      }
} 
	    


