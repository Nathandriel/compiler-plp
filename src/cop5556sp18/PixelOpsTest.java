package cop5556sp18;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

class PixelOpsTest {

	@Test
	public void pixelOpsTest() {
		int pixel = 0xFFFFFFFF;
		int alpha = RuntimePixelOps.getSample(pixel, RuntimePixelOps.ALPHA);
		assertEquals(0xFF,alpha);
		int red = RuntimePixelOps.getSample(pixel, RuntimePixelOps.RED);
		assertEquals(0xFF,red);
		int green = RuntimePixelOps.getSample(pixel, RuntimePixelOps.GREEN);
		assertEquals(0xFF,green);
		int blue = RuntimePixelOps.getSample(pixel, RuntimePixelOps.BLUE);
		assertEquals(0xFF,blue);	
		int pixel2 = 0x0A0B0C0D;
		alpha = RuntimePixelOps.getSample(pixel2, RuntimePixelOps.ALPHA);
		assertEquals(0x0A,alpha);
		red = RuntimePixelOps.getSample(pixel2, RuntimePixelOps.RED);
		assertEquals(0x0B,red);
		green = RuntimePixelOps.getSample(pixel2, RuntimePixelOps.GREEN);
		assertEquals(0x0C,green);
		blue = RuntimePixelOps.getSample(pixel2, RuntimePixelOps.BLUE);
		assertEquals(0x0D,blue);
		int pixel3 = 0xA0B0C0D0;
		alpha = RuntimePixelOps.getSample(pixel3, RuntimePixelOps.ALPHA);
		assertEquals(0xA0,alpha);
		red = RuntimePixelOps.getSample(pixel3, RuntimePixelOps.RED);
		assertEquals(0xB0,red);
		green = RuntimePixelOps.getSample(pixel3, RuntimePixelOps.GREEN);
		assertEquals(0xC0,green);
		blue = RuntimePixelOps.getSample(pixel3, RuntimePixelOps.BLUE);
		assertEquals(0xD0,blue);
		int pixel4 = 0xFAFBFCFD;
		alpha = RuntimePixelOps.getSample(pixel4, RuntimePixelOps.ALPHA);
		assertEquals(0xFA,alpha);
		red = RuntimePixelOps.getSample(pixel4, RuntimePixelOps.RED);
		assertEquals(0xFB,red);
		green = RuntimePixelOps.getSample(pixel4, RuntimePixelOps.GREEN);
		assertEquals(0xFC,green);
		blue = RuntimePixelOps.getSample(pixel4, RuntimePixelOps.BLUE);
		assertEquals(0xFD,blue);	
	}
}
