package name.kazennikov.dafsa;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.antlr.stringtemplate.StringTemplate;

import com.google.common.io.Files;

public class StGen {
	
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Var {
		@XmlAttribute
		String name;
		@XmlAttribute
		String value;
	}
	
	@XmlRootElement(name = "vars")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class VarConfig {
		@XmlElement(name = "var")
		List<Var> vars;		
	}
	
	public static void main(String[] args) throws JAXBException, IOException {
		if(args.length < 3) {
			System.out.printf("Usage: StGen config.xml template outfile%n");
			System.exit(0);
		}
		
		JAXBContext ctx = JAXBContext.newInstance(VarConfig.class);
		Unmarshaller um = ctx.createUnmarshaller();
		
		VarConfig vc = (VarConfig) um.unmarshal(new File(args[0]));
		
		Map<String, String> vars = new HashMap<String, String>();
		
		for(Var v : vc.vars) {
			vars.put(v.name, v.value);
		}
		
		String template = Files.toString(new File(args[1]), Charset.defaultCharset());
		
		StringTemplate st = new StringTemplate(template);
		
		st.setAttributes(vars);
		
		Files.write(st.toString(), new File(args[2]), Charset.defaultCharset());
	
		
		
	}

}
