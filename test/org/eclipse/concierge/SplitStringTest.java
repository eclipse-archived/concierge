package org.eclipse.concierge;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SplitStringTest {

	private static String longTest = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse vestibulum, arcu non sagittis aliquam, nulla mauris tincidunt leo, in eleifend erat quam eu ante. Donec enim mauris, vulputate ac mattis vitae, mattis ut urna. Praesent tincidunt, tellus eget hendrerit suscipit, magna elit dapibus nibh, quis hendrerit metus leo id leo. Vestibulum venenatis gravida erat, nec lacinia mi consectetur elementum. Mauris consectetur imperdiet metus, a porta diam facilisis sed. Aenean dolor dolor, rutrum a facilisis auctor, iaculis eu velit. Duis egestas, urna eu condimentum tristique, tellus turpis faucibus nisl, vel lacinia neque erat quis tortor. Nulla facilisi. Aliquam quis sapien sit amet orci sodales suscipit sed id tortor. Ut fringilla dui id tellus vestibulum eget vehicula odio commodo. Praesent ultrices dignissim pretium. Quisque pellentesque magna ut ipsum ornare adipiscing. Sed bibendum est vehicula leo laoreet tempus. Curabitur porta molestie odio a sollicitudin. Duis sit amet nisi lacus. Aenean a metus vel mauris blandit imperdiet. Suspendisse ac commodo erat. Aliquam vehicula volutpat enim ac porttitor. Vivamus eros nibh, hendrerit vel mollis ut, auctor ac purus. Duis convallis enim eget diam sollicitudin ornare. Aenean sapien augue, venenatis eu accumsan nec, porttitor ac velit. Sed mattis est sit amet nibh faucibus ultricies. Vivamus dui tortor, vulputate non auctor vitae, hendrerit eu nunc. Morbi sit amet tellus mi. Integer vitae turpis enim. Curabitur id sem quam. Aliquam ut dui et nisl sollicitudin malesuada vitae in tortor. Suspendisse eget leo sit amet lacus mollis lacinia. Nulla facilisi. Nunc sagittis, quam nec varius adipiscing, tortor ante elementum nulla, non egestas lorem lacus vitae tellus. In tortor risus, cursus elementum bibendum sed, suscipit sit amet orci. Sed dapibus condimentum urna eget posuere. Integer volutpat arcu vel massa eleifend nec vehicula purus sollicitudin. Nam mauris est, aliquam fermentum hendrerit eu, pharetra vitae nibh. Mauris laoreet dictum quam quis feugiat. Duis condimentum tincidunt lorem et porta. Duis dapibus imperdiet nunc ut pulvinar. Phasellus eu turpis ornare mi blandit accumsan eu et libero. Fusce quis purus quam. Sed commodo lobortis dolor, non iaculis mi dapibus sit amet. Cras tincidunt congue nibh non varius. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; In hac habitasse platea dictumst. Nunc molestie tellus a diam interdum ut ultricies elit interdum. Duis ultricies facilisis tempus. Nam porta scelerisque dolor eu feugiat. Etiam eu tortor lacus, vitae commodo quam. Duis nec velit erat. In ac mi sit amet diam elementum egestas. Fusce urna lectus, congue at rutrum et, venenatis eu massa. Aliquam at mi non lectus rhoncus dictum. Donec eu dui at massa convallis molestie. Aenean felis turpis, placerat at convallis vitae, ornare sit amet velit. Praesent pellentesque magna at nibh cursus vitae viverra erat auctor. Vestibulum rutrum felis quis erat dapibus vehicula. Fusce rutrum, magna sed mollis tincidunt, magna quam auctor eros, vitae euismod erat nulla vitae massa. Mauris at placerat eros. Donec ullamcorper euismod eros id egestas. Donec vitae odio dui. Aenean vulputate dignissim justo, sit amet mollis neque auctor id. Fusce lacinia, velit sit amet elementum placerat, diam leo ullamcorper dolor, vel rhoncus justo diam eget velit. Sed quis mi leo. Duis luctus, metus vitae convallis scelerisque, eros tellus congue neque, eget interdum quam arcu sed tellus. Suspendisse bibendum porta viverra. Suspendisse sit amet lobortis urna. Cras lorem felis, egestas vitae volutpat quis, sagittis quis est. Nullam gravida fermentum vestibulum. Sed pharetra nunc ut est sagittis tincidunt. Morbi lobortis rutrum quam a iaculis. Fusce mattis accumsan ornare. Pellentesque magna ipsum, ultrices id dignissim in, rhoncus vel lorem. Vivamus elit metus, imperdiet interdum malesuada at, faucibus vel magna. Suspendisse interdum magna id libero lobortis egestas. Quisque at urna elit. In hac habitasse platea dictumst. Sed sit amet pulvinar libero. Etiam condimentum vehicula sapien et fermentum. Nulla vitae dui at nulla eleifend pharetra id sed purus. In hac habitasse platea dictumst. Vestibulum laoreet, lorem et varius imperdiet, purus lectus posuere nibh, tincidunt suscipit nibh nibh eget turpis. Duis bibendum sapien a urna eleifend lacinia sollicitudin felis sagittis. Suspendisse magna odio, tincidunt sit amet varius ac, consequat quis tortor. Maecenas at congue odio. Nunc quis felis nulla, id varius orci. Cras sapien purus, euismod ac consequat vitae, aliquam eu augue. Nulla facilisi. Quisque mi quam, facilisis sagittis laoreet nec, tristique nec ipsum. Integer eget ante nulla, sed auctor mauris. Sed eu purus diam, in consectetur sapien. Cras laoreet justo sit amet neque dictum fermentum. Quisque non interdum augue. Nullam imperdiet turpis non risus elementum in pretium est commodo. Nulla pellentesque commodo nibh, nec feugiat risus tincidunt et. Nulla sed erat vel justo convallis pulvinar. Mauris consequat aliquam purus, in mattis diam luctus quis. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Quisque euismod lectus vitae sem porttitor pellentesque. Proin nunc orci, suscipit in porta in, elementum at mauris. Aenean tristique gravida neque, ac adipiscing enim euismod ut. Pellentesque velit lacus, dapibus sed dignissim aliquam, laoreet id lacus. Suspendisse fermentum diam at dolor vehicula facilisis. Quisque sed malesuada mauris. Quisque sapien neque, aliquet ac cursus sed, lobortis vitae quam. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Quisque nibh metus, cursus eu sagittis nec, dapibus vel lorem. Ut porta commodo pretium. Donec id justo et felis eleifend pretium id ut orci. Suspendisse cursus volutpat libero eget interdum. Praesent adipiscing leo ac metus adipiscing pellentesque pellentesque magna auctor. Integer nibh diam, vehicula sit amet sodales a, fermentum sit amet elit. Donec gravida tempor ante, sed bibendum dui tristique a. Vestibulum commodo velit eu lorem euismod sit amet luctus ipsum venenatis. Nunc consectetur tellus et lorem faucibus eget tristique libero venenatis. Nulla sem urna, adipiscing id gravida nec, porttitor ac est. Nam ultrices metus vel odio dapibus et iaculis augue pellentesque. Aliquam sed erat purus. Ut ut nulla tortor. Pellentesque turpis dolor, consequat sed laoreet lobortis, volutpat et leo. Donec eget libero et libero suscipit vehicula. Integer sit amet auctor neque. Ut aliquet lorem ac turpis sodales ornare. Integer a pulvinar augue. Vestibulum ac est eu risus scelerisque consequat. Lorem ipsum dolor sit amet, consectetur adipiscing";

	public static final Pattern SPLIT_AT_COMMA = Pattern.compile(",\\s*(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPerformance() {
		int compare = 0;

		long time = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			String[] foo = SPLIT_AT_COMMA.split(longTest);
			compare = foo.length;
		}
		long time1 = System.nanoTime() - time;

		System.out.println("String.split: " + time1);

		int compare2 = 0;
		time = System.nanoTime();
		for (int i = 0; i < 1000; i++) {
			String[] foo = Utils.splitAtComma(longTest);
			compare2 = foo.length;
		}
		long time2 = System.nanoTime() - time;

		assertEquals(compare, compare2);

		System.out.println("Utils.splitString: " + time2);

		assertTrue(time2 < time1);
		
		System.out.println("difference: " + (time2-time1));
	}
	
}
