import processing.core.PApplet;

public class FollowTest extends PApplet {

	private float t = 0;

	// Test Objekt
	private int x;
	private int y;

	public void setup() {
		size(640, 480);
		smooth();
		frameRate(60);

		x = 200;
		y = height / 2;
	}

	public void move() {
		t += 0.05;
		x += (int) (sin(t) * 10);
		y += (int) (cos(t) * 10);
	}

	public void followText(String text, int x, int y, int width, int height) {
		// Ermitteln welche Rand am weitesten entfernt ist
		int abstand = 50;

		int xAchse = constrain(x - abstand, abstand, width - 50);
		int yAchse = constrain(y - abstand, abstand, height - 50);

		int textsize = (text.length() * 6)+5;
		fill(255,255,255);
		rect(xAchse - 5, yAchse - 10, textsize + 5, 10);
		line(xAchse - 5, yAchse, x, y);
		line(xAchse - 5 + textsize + 5, yAchse, x, y);
		fill(0);
		text(text, xAchse, yAchse);
	}

	public void draw() {
		background(255);

		move();

		fill(255, 0, 0);
		ellipseMode(CENTER);
		ellipse(x, y, 32, 32);
		
		//Am Schluss Text verfolger
		followText("Hallo Welt!", x, y, width, height);
	}
}
