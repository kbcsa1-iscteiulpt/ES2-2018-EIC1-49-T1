/**
 * Copyright (c) 2010-2012, JGraph Ltd
 */
package com.mxgraph.shape;

import org.w3c.dom.Node;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.util.svg.AWTPathProducer;
import com.mxgraph.util.svg.AWTPolygonProducer;
import com.mxgraph.util.svg.AWTPolylineProducer;
import com.mxgraph.util.svg.CSSConstants;
import com.mxgraph.util.svg.ExtendedGeneralPath;
import com.mxgraph.view.mxCellState;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Stencil shape drawing that takes an XML definition of the shape and renders
 * it.
 * 
 * See http://projects.gnome.org/dia/custom-shapes for specs. See
 * http://dia-installer.de/shapes_de.html for shapes.
 */
public class mxStencilShape extends mxBasicShape
{

	private mxStencilShapeProduct mxStencilShapeProduct = new mxStencilShapeProduct();

	public static final Logger log = Logger.getLogger(mxStencilShape.class.getName());

	public mxStencilShape()
	{
		super();
	}

	protected GeneralPath shapePath;

	/**
	 * Reference to the root node of the Dia shape description.
	 */
	protected Node root;

	protected svgShape rootShape;

	protected Rectangle2D boundingBox;

	protected String name;

	protected String iconPath;

	/**
	 * Transform cached to save instance created. Used to scale the internal
	 * path of shapes where possible
	 */
	protected AffineTransform cachedTransform = new AffineTransform();

	/**
	 * Constructs a new stencil for the given Dia shape description.
	 */
	public mxStencilShape(String shapeXml)
	{
		this(mxXmlUtils.parseXml(shapeXml));
	}

	public mxStencilShape(Document document)
	{
		if (document != null)
		{
			NodeList nameList = document.getElementsByTagName("name");

			if (nameList != null && nameList.getLength() > 0)
			{
				this.name = nameList.item(0).getTextContent();
			}

			NodeList iconList = document.getElementsByTagName("icon");

			if (iconList != null && iconList.getLength() > 0)
			{
				this.iconPath = iconList.item(0).getTextContent();
			}

			NodeList svgList = document.getElementsByTagName("svg:svg");

			if (svgList != null && svgList.getLength() > 0)
			{
				this.root = svgList.item(0);
			}
			else
			{
				svgList = document.getElementsByTagName("svg");

				if (svgList != null && svgList.getLength() > 0)
				{
					this.root = svgList.item(0);
				}
			}

			if (this.root != null)
			{
				rootShape = new svgShape(null, null);
				createShape(this.root, rootShape);
			}
		}
	}

	/**
	 * 
	 */
	@Override
	public void paintShape(mxGraphics2DCanvas canvas, mxCellState state)
	{
		double x = state.getX();
		double y = state.getY();
		double w = state.getWidth();
		double h = state.getHeight();

		canvas.getGraphics().translate(x, y);
		double widthRatio = 1;
		double heightRatio = 1;

		if (boundingBox != null)
		{
			widthRatio = w / boundingBox.getWidth();
			heightRatio = h / boundingBox.getHeight();
		}

		this.paintNode(canvas, state, rootShape, widthRatio, heightRatio);

		canvas.getGraphics().translate(-x, -y);
	}

	/**
	 * 
	 */
	public void paintNode(mxGraphics2DCanvas canvas, mxCellState state,
			svgShape shape, double widthRatio, double heightRatio)
	{
		Shape associatedShape = shape.shape;

		boolean fill = false;
		boolean stroke = true;
		Color fillColor = null;
		Color strokeColor = null;

		Map<String, Object> style = shape.style;

		if (style != null)
		{
			String fillStyle = mxUtils.getString(style,
					CSSConstants.CSS_FILL_PROPERTY);
			String strokeStyle = mxUtils.getString(style,
					CSSConstants.CSS_STROKE_PROPERTY);

			if (strokeStyle != null
					&& strokeStyle.equals(CSSConstants.CSS_NONE_VALUE))
			{
				if (strokeStyle.equals(CSSConstants.CSS_NONE_VALUE))
				{
					stroke = false;
				}
				else if (strokeStyle.trim().startsWith("#"))
				{
					int hashIndex = strokeStyle.indexOf("#");
					strokeColor = mxUtils.parseColor(strokeStyle
							.substring(hashIndex + 1));
				}
			}

			if (fillStyle != null)
			{
				if (fillStyle.equals(CSSConstants.CSS_NONE_VALUE))
				{
					fill = false;
				}
				else if (fillStyle.trim().startsWith("#"))
				{
					int hashIndex = fillStyle.indexOf("#");
					fillColor = mxUtils.parseColor(fillStyle
							.substring(hashIndex + 1));
					fill = true;
				}
				else
				{
					fill = true;
				}
			}
		}

		if (associatedShape != null)
		{
			boolean wasScaled = false;

			if (widthRatio != 1 || heightRatio != 1)
			{
				transformShape(associatedShape, 0.0, 0.0, widthRatio,
						heightRatio);
				wasScaled = true;
			}

			// Paints the background
			if (fill && configureGraphics(canvas, state, true))
			{
				if (fillColor != null)
				{
					canvas.getGraphics().setColor(fillColor);
				}

				canvas.getGraphics().fill(associatedShape);
			}

			// Paints the foreground
			if (stroke && configureGraphics(canvas, state, false))
			{
				if (strokeColor != null)
				{
					canvas.getGraphics().setColor(strokeColor);
				}

				canvas.getGraphics().draw(associatedShape);
			}

			if (wasScaled)
			{
				transformShape(associatedShape, 0.0, 0.0, 1.0 / widthRatio,
						1.0 / heightRatio);
			}
		}

		/*
		 * If root is a group element, then we should add it's styles to the
		 * children.
		 */
		for (svgShape subShape : shape.subShapes)
		{
			paintNode(canvas, state, subShape, widthRatio, heightRatio);
		}
	}

	/**
	 * Scales the points composing this shape by the x and y ratios specified
	 * 
	 * @param shape
	 *            the shape to scale
	 * @param transX
	 *            the x translation
	 * @param transY
	 *            the y translation
	 * @param widthRatio
	 *            the x co-ordinate scale
	 * @param heightRatio
	 *            the y co-ordinate scale
	 */
	protected void transformShape(Shape shape, double transX, double transY,
			double widthRatio, double heightRatio)
	{
		if (shape instanceof Rectangle2D)
		{
			Rectangle2D rect = (Rectangle2D) shape;
			if (transX != 0 || transY != 0)
			{
				rect.setFrame(rect.getX() + transX, rect.getY() + transY,
						rect.getWidth(), rect.getHeight());
			}

			if (widthRatio != 1 || heightRatio != 1)
			{
				rect.setFrame(rect.getX() * widthRatio, rect.getY()
						* heightRatio, rect.getWidth() * widthRatio,
						rect.getHeight() * heightRatio);
			}
		}
		else if (shape instanceof Line2D)
		{
			Line2D line = (Line2D) shape;
			if (transX != 0 || transY != 0)
			{
				line.setLine(line.getX1() + transX, line.getY1() + transY,
						line.getX2() + transX, line.getY2() + transY);
			}
			if (widthRatio != 1 || heightRatio != 1)
			{
				line.setLine(line.getX1() * widthRatio, line.getY1()
						* heightRatio, line.getX2() * widthRatio, line.getY2()
						* heightRatio);
			}
		}
		else if (shape instanceof GeneralPath)
		{
			GeneralPath path = (GeneralPath) shape;
			cachedTransform.setToScale(widthRatio, heightRatio);
			cachedTransform.translate(transX, transY);
			path.transform(cachedTransform);
		}
		else if (shape instanceof ExtendedGeneralPath)
		{
			ExtendedGeneralPath path = (ExtendedGeneralPath) shape;
			cachedTransform.setToScale(widthRatio, heightRatio);
			cachedTransform.translate(transX, transY);
			path.transform(cachedTransform);
		}
		else if (shape instanceof Ellipse2D)
		{
			Ellipse2D ellipse = (Ellipse2D) shape;
			if (transX != 0 || transY != 0)
			{
				ellipse.setFrame(ellipse.getX() + transX, ellipse.getY()
						+ transY, ellipse.getWidth(), ellipse.getHeight());
			}
			if (widthRatio != 1 || heightRatio != 1)
			{
				ellipse.setFrame(ellipse.getX() * widthRatio, ellipse.getY()
						* heightRatio, ellipse.getWidth() * widthRatio,
						ellipse.getHeight() * heightRatio);
			}
		}
	}

	/**
	 * 
	 */
	public void createShape(Node root, svgShape shape)
	{
		Node child = root.getFirstChild();
		/*
		 * If root is a group element, then we should add it's styles to the
		 * childrens...
		 */
		while (child != null)
		{
			if (isGroup(child.getNodeName()))
			{
				String style = ((Element) root).getAttribute("style");
				Map<String, Object> styleMap = mxStencilShape
						.getStylenames(style);
				svgShape subShape = new svgShape(null, styleMap);
				createShape(child, subShape);
			}

			svgShape subShape = mxStencilShapeProduct.createElement(child, this);

			if (subShape != null)
			{
				shape.subShapes.add(subShape);
			}
			child = child.getNextSibling();
		}

		for (svgShape subShape : shape.subShapes)
		{
			if (subShape != null && subShape.shape != null)
			{
				if (boundingBox == null)
				{
					boundingBox = subShape.shape.getBounds2D();
				}
				else
				{
					boundingBox.add(subShape.shape.getBounds2D());
				}
			}
		}

		// If the shape does not butt up against either or both axis,
		// ensure it is flush against both
		if (boundingBox != null
				&& (boundingBox.getX() != 0 || boundingBox.getY() != 0))
		{
			for (svgShape subShape : shape.subShapes)
			{
				if (subShape != null && subShape.shape != null)
				{
					transformShape(subShape.shape, -boundingBox.getX(),
							-boundingBox.getY(), 1.0, 1.0);
				}
			}
		}
	}

	/**
	 * Forms an internal representation of the specified SVG element and returns
	 * that representation
	 * 
	 * @param root
	 *            the SVG element to represent
	 * @return the internal representation of the element, or null if an error
	 *         occurs
	 */
	public svgShape createElement(Node root)
	{
		return mxStencilShapeProduct.createElement(root, this);
	}

	private boolean isGroup(String tag)
	{
		return tag.equals("svg:g") || tag.equals("g");
	}

	protected class svgShape
	{
		public Shape shape;

		/**
		 * Contains an array of key, value pairs that represent the style of the
		 * cell.
		 */
		protected Map<String, Object> style;

		public List<svgShape> subShapes;

		/**
		 * Holds the current value to which the shape is scaled in X
		 */
		protected double currentXScale;

		/**
		 * Holds the current value to which the shape is scaled in Y
		 */
		protected double currentYScale;

		public svgShape(Shape shape, Map<String, Object> style)
		{
			this.shape = shape;
			this.style = style;
			subShapes = new ArrayList<svgShape>();
		}

		public double getCurrentXScale()
		{
			return currentXScale;
		}

		public void setCurrentXScale(double currentXScale)
		{
			this.currentXScale = currentXScale;
		}

		public double getCurrentYScale()
		{
			return currentYScale;
		}

		public void setCurrentYScale(double currentYScale)
		{
			this.currentYScale = currentYScale;
		}
	}

	/**
	 * Returns the stylenames in a style of the form stylename[;key=value] or an
	 * empty array if the given style does not contain any stylenames.
	 * 
	 * @param style
	 *            String of the form stylename[;stylename][;key=value].
	 * @return Returns the stylename from the given formatted string.
	 */
	protected static Map<String, Object> getStylenames(String style)
	{
		if (style != null && style.length() > 0)
		{
			Map<String, Object> result = new Hashtable<String, Object>();

			if (style != null)
			{
				String[] pairs = style.split(";");

				for (int i = 0; i < pairs.length; i++)
				{
					String[] keyValue = pairs[i].split(":");

					if (keyValue.length == 2)
					{
						result.put(keyValue[0].trim(), keyValue[1].trim());
					}
				}
			}
			return result;
		}

		return null;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getIconPath()
	{
		return iconPath;
	}

	public void setIconPath(String iconPath)
	{
		this.iconPath = iconPath;
	}

	public Rectangle2D getBoundingBox()
	{
		return boundingBox;
	}

	public void setBoundingBox(Rectangle2D boundingBox)
	{
		this.boundingBox = boundingBox;
	}
}
