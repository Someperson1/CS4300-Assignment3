package sgraph;

import main.Raytracer;
import org.joml.Matrix4f;

import org.joml.Vector4f;
import util.Light;
import util.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * This node represents a transformation in the scene graph. It has only one
 * child. The transformation can be viewed as changing from its child's
 * coordinate system to its parent's coordinate system This also stores an
 * animation transform that can be tweaked at runtime
 *
 * @author Amit Shesh
 */
public class TransformNode extends AbstractNode {
    /**
     * Matrices storing the static and animation transformations separately, so
     * that they can be changed separately
     */
    protected Matrix4f transform, animation_transform;

    /**
     * A reference to its only child
     */
    INode child;

    public TransformNode(IScenegraph graph, String name) {
        super(graph, name);
        this.transform = new Matrix4f();
        animation_transform = new Matrix4f();
        child = null;
    }

    /**
     * Creates a deep copy of the subtree rooted at this node
     *
     * @return a deep copy of the subtree rooted at this node
     */
    @Override
    public INode clone() {
        INode newchild;

        if (child != null) {
            newchild = child.clone();
        } else {
            newchild = null;
        }

        TransformNode newtransform = new TransformNode(scenegraph, name);
        newtransform.setTransform(this.transform);
        newtransform.setAnimationTransform(animation_transform);

        if (newchild != null) {
            try {
                newtransform.addChild(newchild);
            } catch (IllegalArgumentException e) {

            }
        }
        return newtransform;
    }

    /**
     * Determines if this node has the specified name and returns itself if so.
     * Otherwise it recurses into its only child
     *
     * @param name name of node to be searched
     */
    public INode getNode(String name) {
        INode n = super.getNode(name);
        if (n != null)
            return n;

        if (child != null) {
            return child.getNode(name);
        }

        return null;
    }

    /**
     * Since this node can have a child, it override this method and adds the
     * child to itself This will overwrite any children set for this node
     * previously.
     *
     * @param child the child of this node
     * @throws IllegalArgumentException this method does not throw this exception
     */
    public void addChild(INode child) throws IllegalArgumentException {
        if (this.child != null)
            throw new IllegalArgumentException("Transform node already has a child");
        this.child = child;
        this.child.setParent(this);
    }

    /**
     * Draws the scene graph rooted at this node After preserving the current top
     * of the modelview stack, this "post-multiplies" its animation transform and
     * then its transform in that order to the top of the model view stack, and
     * then recurses to its child. When the child is drawn, it restores the
     * modelview matrix
     *
     * @param context   the generic renderer context {@link IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     */

    @Override
    public void draw(IScenegraphRenderer context, Stack<Matrix4f> modelView) {
        modelView.push(new Matrix4f(modelView.peek()));
        modelView.peek().mul(animation_transform)
                .mul(transform);
        if (child != null)
            child.draw(context, modelView);
        modelView.pop();
    }


    /**
     * Sets the animation transform of this node
     *
     * @param mat the animation transform of this node
     */
    public void setAnimationTransform(Matrix4f mat) {
        animation_transform = new Matrix4f(mat);
    }

    /**
     * Gets the transform at this node (not the animation transform)
     */
    public Matrix4f getTransform() {
        return transform;
    }

    /**
     * Sets the transformation of this node
     */
    @Override
    public void setTransform(Matrix4f t) throws IllegalArgumentException {
        this.transform = new Matrix4f(t);
    }

    /**
     * Gets the animation transform of this node
     */
    Matrix4f getAnimationTransform() {
        return animation_transform;
    }

    /**
     * Sets the scene graph object of which this node is a part, and then recurses
     * to its child
     *
     * @param graph a reference to the scenegraph object of which this tree is a
     *              part
     */
    @Override
    public void setScenegraph(IScenegraph graph) {
        super.setScenegraph(graph);
        if (child != null) {
            child.setScenegraph(graph);
        }
    }

    /**
     * Overridden version from @link{AbstractNode}. This version first modifies
     * the modelview matrix to include its transformation and animation
     * transformation. Then it collects all the lights from its child, and then
     * appends to them the lights from this node as well.
     * <p>
     * It uses the original version for getting the lights in this node.
     */
    @Override
    public List<Light> getLightsInView(Stack<Matrix4f> modelview) {
        List<Light> lights = new ArrayList<Light>();

        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek().mul(animation_transform)
                .mul(transform);
        if (child != null)
            lights.addAll(child.getLightsInView(modelview));
        modelview.pop();

        //now get the lights from this node's lights
        lights.addAll(super.getLightsInView(modelview));
        return lights;
    }

    @Override
    public List<Float> raytrace(Raytracer raytracer, Vector4f rayOrigin, Vector4f rayDirection) {
        // Makes sure that the ray is in the object's coordinate system
        Matrix4f inverse = new Matrix4f().identity().mul(animation_transform).mul(transform);
        inverse = inverse.invert();
        return child.raytrace(raytracer, rayOrigin.mul(inverse),
                rayDirection.mul(inverse));
    }

    @Override
    public List<Material> getObjectMaterial() {
        return child.getObjectMaterial();
    }
}
