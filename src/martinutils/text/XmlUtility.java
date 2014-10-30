package martinutils.text;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Classe utilitaria per XML
 * @author Martin
 */
public class XmlUtility
{
	private static Transformer transformer;
	private static XPath xpath;
	private static DocumentBuilderFactory factory;
	private static DocumentBuilder builder;
	
	/**
	 * Prints the entire content of a node as plain text, comprehensive of the node itself and all tags/attributes.
	 * Please note that it will not print newlines!
	 * @param node The node to print
	 * @return The printed node
	 * @throws TransformerException
	 */
	public static String printNodeContent(Node node) throws TransformerException 
	{	
		String result;
		// If it is a textnode, just return its content after escaping entities (whici is necessary because this extractor extracts segments of text mixed with tags)
		if (node.getNodeName().equals("#text"))
		{
			String textContent = node.getTextContent();
			result = XMLEntities.xmlEntities(textContent, XMLEntities.MODE.NODE);
		}
		else
		{
			// Build a StreamResult
			StringWriter sw = new StringWriter();
			StreamResult streamRes = new StreamResult(sw);
			
			// Initialize transformer
			if (transformer == null)
			{
				transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.setOutputProperty(OutputKeys.INDENT, "no");
			}
			
			// Transform into string the DOM of the node
			DOMSource source = new DOMSource(node);
			transformer.transform(source, streamRes);
			
			// Save the result and close the writer
			result = sw.toString();

			try{ sw.close(); } catch(IOException e){ }
			
		}

		// Remove newlines before returning, The option INDENT=no cares for xml indentation but if a text node has a carriare return, this fix will do the job 
		result = result.replace("\n", "").replace("\r", "");
		return result;
	}
	
	/**
	 * Print the inner node of a content by printing the content of all his children
	 * @param node the node to print
	 * @return the pritned node
	 * @throws TransformerException
	 */
	public static String printNodeInnerContent( Node node ) throws TransformerException
	{
		NodeList list = node.getChildNodes();
		int len = list.getLength();
		StringBuilder sb = new StringBuilder();
		
		if ( len > 0 ) {
			for ( int i = 0; i < len; i++ )	{
				Node n = list.item(i);
				sb.append( printNodeContent(n) );
			}
		}	
		
		return sb.toString();
	}

	/**
	 * Restituisce il valore di un attributo XML, oppure null se non trovato
	 */
	public static String getXmlAttributeValue(NamedNodeMap attrs, String attrName)
	{
		if (attrs == null)
			return null;
		
		Node attr = attrs.getNamedItem(attrName);
		if (attr == null)
			return null;
		
		return attr.getNodeValue();
	}
	
	/**
	 * Restituisce il valore di un attributo XML, oppure null se non trovato
	 */
	public static String getXmlAttributeValue(Node node, String attrName)
	{
		if (node == null)
			return null;
		
		NamedNodeMap attrs = node.getAttributes();
		String result = getXmlAttributeValue(attrs, attrName);
		return result;
	}

	/**
	 * Restituisce il primo elemento figlio trovato col nome specificato
	 * @param node il nodo in cui cercare i figli
	 * @param nodeName il nome da cercare
	 * @return
	 */
	public static Node getFirstChildByName(Node node, String nodeName, boolean caseSensitive)
	{
		if (StringUtil.isEmptyOrNull(nodeName))
			return null;
		if (node == null)
			return null;
		
		NodeList children = node.getChildNodes();
		int len = children.getLength();
		
		for (int i = 0; i < len; i++)
		{
			Node child = children.item(i);
			String childNodeName = child.getNodeName();
			
			if (caseSensitive && nodeName.equals(childNodeName))
				return child;
			if (!caseSensitive && nodeName.equalsIgnoreCase(childNodeName))
				return child;
		}
		
		return null;
	}
	
	public static NodeList doXPathQuery(String query, Document doc) throws XPathExpressionException
	{
		if (XmlUtility.xpath == null)
			XmlUtility.xpath = XPathFactory.newInstance().newXPath();
		return (NodeList) XmlUtility.xpath.evaluate(query, doc, XPathConstants.NODESET);
	}
	
	public static void saveXmlFile(Document doc, File file) throws TransformerException
	{
		TransformerFactory transFactory = TransformerFactory.newInstance();  
		Transformer transformer;
		
		try {
			transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "merckmanuals-translations.dtd");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.VERSION, "1.0");
			transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			Result dest = new StreamResult(file);  
			transformer.transform(new DOMSource(doc), dest); 
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
			System.exit(-1);
		} 
	}
	
	public static Document readXmlFile(File file) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(file);
        //doc.getDocumentElement().normalize();
        
        return doc;
	}
	
	public static Document readXml(String xml) throws ParserConfigurationException, SAXException, IOException
	{
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = dBuilder.parse(is);
        //doc.getDocumentElement().normalize();
        
        return doc;
	}
	
	public static DocumentBuilder getDefaultBuilder() throws ParserConfigurationException
	{
		if (builder == null)
		{
			factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);
			factory.setFeature("http://xml.org/sax/features/namespaces", false);
			factory.setFeature("http://xml.org/sax/features/validation", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			builder = factory.newDocumentBuilder();
		}
		return builder;
	}
}
