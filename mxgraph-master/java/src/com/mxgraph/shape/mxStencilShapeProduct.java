/**
 * Copyright (c) 2010-2012, JGraph Ltd
 */
package com.mxgraph.shape;


import com.mxgraph.shape.mxStencilShape.svgShape;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.util.Map;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D;
import java.util.logging.Level;
import java.awt.geom.Line2D;
import java.awt.Shape;
import com.mxgraph.util.svg.AWTPolygonProducer;
import java.awt.geom.GeneralPath;
import com.mxgraph.util.svg.AWTPolylineProducer;
import java.awt.geom.Ellipse2D;
import com.mxgraph.util.svg.AWTPathProducer;
import java.awt.geom.Path2D;

public class mxStencilShapeProduct {
	/**
	* Forms an internal representation of the specified SVG element and returns that representation
	* @param root the SVG element to represent
	* @return  the internal representation of the element, or null if an error occurs
	*/
	public svgShape createElement(Node root, mxStencilShape mxStencilShape) {
		Element element = null;
		if (root instanceof Element) {
			element = (Element) root;
			String style = element.getAttribute("style");
			Map<String, Object> styleMap = mxStencilShape.getStylenames(style);
			if (isRectangle(root.getNodeName())) {
				svgShape rectShape = null;
				try {
					String xString = element.getAttribute("x");
					String yString = element.getAttribute("y");
					String widthString = element.getAttribute("width");
					String heightString = element.getAttribute("height");
					double x = 0;
					double y = 0;
					double width = 0;
					double height = 0;
					if (xString.length() > 0) {
						x = Double.valueOf(xString);
					}
					if (yString.length() > 0) {
						y = Double.valueOf(yString);
					}
					if (widthString.length() > 0) {
						width = Double.valueOf(widthString);
						if (width < 0) {
							return null;
						}
					}
					if (heightString.length() > 0) {
						height = Double.valueOf(heightString);
						if (height < 0) {
							return null;
						}
					}
					String rxString = element.getAttribute("rx");
					String ryString = element.getAttribute("ry");
					double rx = 0;
					double ry = 0;
					if (rxString.length() > 0) {
						rx = Double.valueOf(rxString);
						if (rx < 0) {
							return null;
						}
					}
					if (ryString.length() > 0) {
						ry = Double.valueOf(ryString);
						if (ry < 0) {
							return null;
						}
					}
					if (rx > 0 || ry > 0) {
						if (rx > 0 && ryString.length() == 0) {
							ry = rx;
						} else if (ry > 0 && rxString.length() == 0) {
							rx = ry;
						}
						if (rx > width / 2.0) {
							rx = width / 2.0;
						}
						if (ry > height / 2.0) {
							ry = height / 2.0;
						}
						rectShape = mxStencilShape.new svgShape(
								new RoundRectangle2D.Double(x, y, width, height, rx, ry), styleMap);
					} else {
						rectShape = mxStencilShape.new svgShape(new Rectangle2D.Double(x, y, width, height), styleMap);
					}
				} catch (Exception e) {
					mxStencilShape.log.log(Level.SEVERE, "Failed to create SVG element", e);
				}
				return rectShape;
			} else if (isLine(root.getNodeName())) {
				String x1String = element.getAttribute("x1");
				String x2String = element.getAttribute("x2");
				String y1String = element.getAttribute("y1");
				String y2String = element.getAttribute("y2");
				double x1 = 0;
				double x2 = 0;
				double y1 = 0;
				double y2 = 0;
				if (x1String.length() > 0) {
					x1 = Double.valueOf(x1String);
				}
				if (x2String.length() > 0) {
					x2 = Double.valueOf(x2String);
				}
				if (y1String.length() > 0) {
					y1 = Double.valueOf(y1String);
				}
				if (y2String.length() > 0) {
					y2 = Double.valueOf(y2String);
				}
				svgShape lineShape = mxStencilShape.new svgShape(new Line2D.Double(x1, y1, x2, y2), styleMap);
				return lineShape;
			} else if (isPolyline(root.getNodeName()) || isPolygon(root.getNodeName())) {
				String pointsString = element.getAttribute("points");
				Shape shape;
				if (isPolygon(root.getNodeName())) {
					shape = AWTPolygonProducer.createShape(pointsString, GeneralPath.WIND_NON_ZERO);
				} else {
					shape = AWTPolylineProducer.createShape(pointsString, GeneralPath.WIND_NON_ZERO);
				}
				if (shape != null) {
					return mxStencilShape.new svgShape(shape, styleMap);
				}
				return null;
			} else if (isCircle(root.getNodeName())) {
				double cx = 0;
				double cy = 0;
				double r = 0;
				String cxString = element.getAttribute("cx");
				String cyString = element.getAttribute("cy");
				String rString = element.getAttribute("r");
				if (cxString.length() > 0) {
					cx = Double.valueOf(cxString);
				}
				if (cyString.length() > 0) {
					cy = Double.valueOf(cyString);
				}
				if (rString.length() > 0) {
					r = Double.valueOf(rString);
					if (r < 0) {
						return null;
					}
				}
				return mxStencilShape.new svgShape(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2), styleMap);
			} else if (isEllipse(root.getNodeName())) {
				double cx = 0;
				double cy = 0;
				double rx = 0;
				double ry = 0;
				String cxString = element.getAttribute("cx");
				String cyString = element.getAttribute("cy");
				String rxString = element.getAttribute("rx");
				String ryString = element.getAttribute("ry");
				if (cxString.length() > 0) {
					cx = Double.valueOf(cxString);
				}
				if (cyString.length() > 0) {
					cy = Double.valueOf(cyString);
				}
				if (rxString.length() > 0) {
					rx = Double.valueOf(rxString);
					if (rx < 0) {
						return null;
					}
				}
				if (ryString.length() > 0) {
					ry = Double.valueOf(ryString);
					if (ry < 0) {
						return null;
					}
				}
				return mxStencilShape.new svgShape(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2), styleMap);
			} else if (isPath(root.getNodeName())) {
				String d = element.getAttribute("d");
				Shape pathShape = AWTPathProducer.createShape(d, GeneralPath.WIND_NON_ZERO);
				return mxStencilShape.new svgShape(pathShape, styleMap);
			}
		}
		return null;
	}

	public boolean isRectangle(String tag) {
		return tag.equals("svg:rect") || tag.equals("rect");
	}

	public boolean isPath(String tag) {
		return tag.equals("svg:path") || tag.equals("path");
	}

	public boolean isEllipse(String tag) {
		return tag.equals("svg:ellipse") || tag.equals("ellipse");
	}

	public boolean isLine(String tag) {
		return tag.equals("svg:line") || tag.equals("line");
	}

	public boolean isPolyline(String tag) {
		return tag.equals("svg:polyline") || tag.equals("polyline");
	}

	public boolean isCircle(String tag) {
		return tag.equals("svg:circle") || tag.equals("circle");
	}

	public boolean isPolygon(String tag) {
		return tag.equals("svg:polygon") || tag.equals("polygon");
	}
}