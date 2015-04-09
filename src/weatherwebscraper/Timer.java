/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package weatherwebscraper;

/**
 *
 * @author 422
 */
public class Timer {
    
    long startTime;
    long splitTime;
    
    public Timer() {
        startTime = System.currentTimeMillis();
        splitTime = startTime;
    }
    
    public void printTimeElapsedMessage(String message) {
        System.out.println(message + " took " + (System.currentTimeMillis() - splitTime) + "ms.");
        splitTime = System.currentTimeMillis();
    }
    
    public void printTotalTimeElapsed() {
        System.out.println("Total time elapsed " + (System.currentTimeMillis() - startTime) + "ms.");
    }
}
