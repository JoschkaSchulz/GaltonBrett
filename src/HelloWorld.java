import processing.core.PApplet;


public class HelloWorld extends PApplet {
	public void setup() {
		size(640,480);
		frameRate(60);
		smooth();
	}
	
	private int x = 0;
	
	public void draw() {
		background(255);
		
		rect(5+x++,240,20,20);
	}
}
