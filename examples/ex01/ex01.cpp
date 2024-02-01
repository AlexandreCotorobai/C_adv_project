/*
 * C++ program that show ex01.adv using the opencv library
 *
 * It corresponds to a free translation of example ex01.adv to a c++ program. 
 * It does not include the definition of automaton, only considering the automaton view.
 * So, some adaptation may be required for a proper use.
 */
#include <iostream>
#include <opencv2/opencv.hpp>
  
/////////////////////////////////////////////////////////////////////////
/* AdvFigure
 * An abstract class representing every figure in a view
 * View is set in real coordinates, using some unit length
 * When drawn in an image (viewport), a conversion is done based in a given scaling transformation.
 * This scaling is defined by two values, scaleFrom and scaleTo.
 * For exemplo, if scaleFrom is 1.0 and scaleTo is 50, 1.0 unit length is converted to 50 pixels.
 */
class AdvFigure
{
public:
    std::string key; 
    cv::Point2d referencePoint = cv::Point2d(0,0);
    bool visible = false;
    cv::Scalar strokeColor = cv::Scalar(0, 0, 0);
    int strokeThickness = 2;

    AdvFigure(std::string k) 
        : key(k) 
    {}

    virtual void draw(cv::Mat &mat, double scaleFrom, uint32_t scaleTo) = 0;
};

/////////////////////////////////////////////////////////////////////////
/*
 * AdvStateFigure
 * A class representing a state figure
 * A state figure is composed of a circle 
 */
class AdvStateFigure : public AdvFigure
{
public:
    bool accepting;
    bool initial;
    double radius = 0.5;

    AdvStateFigure(std::string key, cv::Point2d o)
        : AdvFigure(key)
    {
        accepting = false;
        initial = false;
        referencePoint = o;
    }

    void draw(cv::Mat &mat, double scaleFrom, uint32_t scaleTo)
    {
        /* if not visible do nothing */
        if (not visible) return;

        /* determine center and radius in image coordinates */
        cv::Point2d c = referencePoint / scaleFrom * (double)scaleTo;
        cv::Point center(round(c.x), round(c.y));
        int r = round((radius/scaleFrom)*scaleTo);

        /* draw state shape */
        cv::circle(mat, center, r, strokeColor, strokeThickness);
        if (accepting == true) {
            int r2 = round((0.8*radius/scaleFrom)*scaleTo);
            cv::circle(mat, center, r2, strokeColor, strokeThickness);
        }
        if (initial == true)
        {
            /* ... */
        }

        /* draw label */
        cv::Size sz = cv::getTextSize(key, cv::FONT_HERSHEY_SIMPLEX, 0.8, strokeThickness, NULL);
        cv::putText(mat, key, center + cv::Point(-sz.width/2, sz.height/2), cv::FONT_HERSHEY_SIMPLEX, 0.8, strokeThickness);
    }
};

/////////////////////////////////////////////////////////////////////////
/*
 * AdvTransitionFigure
 * A class representing a transition figure
 * A transition figure is composed of a polyline, representing the arrow, and a label
 */
class AdvTransitionFigure : public AdvFigure
{
public:
    enum Align { CENTERED, LEFT, RIGHT, ABOVE, BELOW };

    std::vector<cv::Point2d> arrowPoints;
    std::string label = "-";
    cv::Point2d labelReferencePoint = cv::Point2d(0.0, 0.0);
    Align labelAlignment = CENTERED;

    AdvTransitionFigure(std::string key, std::string l)
        : AdvFigure(key), label(l)
    {}

    void draw(cv::Mat &mat, double scaleFrom, uint32_t scaleTo)
    {
        /* if not visible do nothing */
        if (not visible) return;

        /* convert arrow's points to image coordinates */
        std::vector<cv::Point> points;
        for (auto &p : arrowPoints)
        {

        }
        for (auto p = arrowPoints.begin(); p != arrowPoints.end(); p++)
        {
            int x = round((p->x/scaleFrom)*scaleTo);
            int y = round((p->y/scaleFrom)*scaleTo);
            points.push_back(cv::Point(x, y));
        }

        /* draw the arrow, assuming there are at least 2 points */
        auto p1 = points.begin(), p2 = p1 + 1;
        for (auto p3 = p2 + 1; p3 != points.end(); p1 = p2, p2 = p3, p3++)
        {
            cv::line(mat, *p1, *p2, strokeColor, strokeThickness);
        }
        cv::arrowedLine(mat, *p1, *p2, strokeColor, strokeThickness);

        /* draw the label (only CENTERED and LEFT alignments are implemented) */
        cv::Size sz = cv::getTextSize(label, cv::FONT_HERSHEY_SIMPLEX, 0.5, strokeThickness, NULL);
        cv::Point2d o2d = (labelReferencePoint / scaleFrom) * (double)scaleTo;
        cv::Point origin = cv::Point(round(o2d.x), round(o2d.y));
        switch (labelAlignment)
        {
            case CENTERED: 
                origin += cv::Point(-sz.width/2, sz.height/2);
                break;
            case LEFT:
            default:
                origin += cv::Point(0, sz.height/2);
                break;
        }
        cv::putText(mat, label, origin, cv::FONT_HERSHEY_SIMPLEX, 0.5, strokeThickness);
    }
};

/////////////////////////////////////////////////////////////////////////

/* AdvLoopTransitionFigure
 * Convenient class to generate the default transition figure for a transition
 * connecting a state with itself,
 */
class AdvLoopTransitionFigure : public AdvTransitionFigure
{
public:
    AdvLoopTransitionFigure(std::string key, std::string label, cv::Point2d p) 
        : AdvTransitionFigure(key, label)
    {
        /* set arrow points */
        p += cv::Point2d(-0.2, -0.6);
        arrowPoints.push_back(p);
        p += cv::Point2d(-0.2, -0.3);
        arrowPoints.push_back(p);
        p += cv::Point2d(0.8, 0.0);
        arrowPoints.push_back(p);
        p += cv::Point2d(-0.2, 0.3);
        arrowPoints.push_back(p);

        /* set label reference point and alignment */
        labelReferencePoint = p + cv::Point2d(0.2, -0.2);
        labelAlignment = LEFT;
    }
};

/////////////////////////////////////////////////////////////////////////

/* AdvLineTransitionFigure
 * Convenient class to generate the default transition figure for transition 
 * connecting two different states.
 */
class AdvLineTransitionFigure : public AdvTransitionFigure
{
public:
    AdvLineTransitionFigure(std::string key, std::string label, cv::Point2d p1, cv::Point2d p2)
        : AdvTransitionFigure(key, label)
    {
        /* set arrow points */
        cv::Point2d p21 = p2 - p1;
        cv::Point2d d = p21 / cv::norm(p21) * 0.7;
        cv::Point2d pa = p1 + d;
        arrowPoints.push_back(pa);
        cv::Point2d pb = p2 - d;
        arrowPoints.push_back(pb);

        /* set label reference point and alignment */
        labelReferencePoint = (pa + pb) / 2 + cv::Point2d(0, -0.2);
        labelAlignment = CENTERED;
    }
};

/////////////////////////////////////////////////////////////////////////

/* AdvAutomatonView
 * An animation view is composed of a set of figures.
 * Figures are stored in a map.
 */
class AdvAutomatonView
{
public:
    std::string name;
    std::map<std::string, AdvFigure*> figures;

    AdvAutomatonView() {}

    void addFigure(std::string key, AdvFigure *figure)
    {
        figures[key] = figure;
    }

    void draw(cv::Mat &mat, double scaleFrom, uint32_t scaleTo)
    {
        for (auto figure : figures)
        {
            figure.second->draw(mat, scaleFrom, scaleTo);
        }
    }
};

/////////////////////////////////////////////////////////////////////////
// Animation code
int main(int argc, char** argv)
{
    /* create an automaton view */
    AdvAutomatonView *av = new AdvAutomatonView();

    /* place states */
    AdvFigure *figure;
    figure = new AdvStateFigure("A", cv::Point2d(2.0, 1.0));
    av->addFigure("A", figure);
    figure = new AdvStateFigure("B", cv::Point2d(5.0, 1.0));
    av->addFigure("B", figure);

    /* transitions not explicitly shaped get default shapes */
    figure = new AdvLoopTransitionFigure("<A,A>", "'a','b','c'", cv::Point2d(2.0, 1.0));
    av->addFigure("<A,A>", figure);
    figure = new AdvLineTransitionFigure("<A,B>", "'a','b'", cv::Point2d(2.0, 1.0), cv::Point2d(5.0, 1.0));
    av->addFigure("<A,B>", figure);

    /* set A as the initial state */
    ((AdvStateFigure*)av->figures["A"])->initial = true;

    /* create the main window (animation support) */
    cv::Mat window(510, 510, CV_8UC3, cv::Scalar(255, 255, 255));

    /* create a viewport (vp1) */
    cv::Mat vp1 = cv::Mat(500, 500, CV_8UC3, cv::Scalar(255, 255, 255));

    /* 1st image */
    av->figures["A"]->visible = true;
    av->figures["B"]->visible = true;
    ((AdvStateFigure*)av->figures["B"])->accepting = false;
    av->draw(vp1, 1.0, 50);
    vp1.copyTo(window(cv::Rect(10, 10, 500, 500)));
    imshow("Animation a1", window);
    cv::waitKey(0);

    /* 2nd image */
    av->figures["<A,B>"]->visible = true;
    av->draw(vp1, 1.0, 50);
    vp1.copyTo(window(cv::Rect(10, 10, 500, 500)));
    imshow("Animation a1", window);
    cv::waitKey(0);

    /* 3rd image */
    av->figures["<A,A>"]->visible = true;
    av->draw(vp1, 1.0, 50);
    vp1.copyTo(window(cv::Rect(10, 10, 500, 500)));
    imshow("Animation a1", window);
    cv::waitKey(0);
  
    /* 4th image */
    ((AdvStateFigure*)av->figures["B"])->accepting = true;
    av->draw(vp1, 1.0, 50);
    vp1.copyTo(window(cv::Rect(10, 10, 500, 500)));
    imshow("Animation a1", window);
    cv::waitKey(0);
  
    return 0;
}
