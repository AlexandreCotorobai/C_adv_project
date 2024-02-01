#!/usr/bin/python3
#
# Python program that show ex01.adv using the opencv library
#
# It corresponds to a free translation of example ex01.adv to a c++ program.
# It does not include the definition of automaton, only considering the automaton view.
# So, some adaptation may be required for a proper use.
#

import cv2 as cv
import numpy as np
import math
from enum import Enum

#--------------------------------------------------------

class Point:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __add__(self, other):
        return Point(self.x + other.x, self.y + other.y)

    def __sub__(self, other):
        return Point(self.x - other.x, self.y - other.y)

    def __mul__(self, scalar):
        return Point(self.x * scalar, self.y * scalar)

    def __truediv__(self, scalar):
        return Point(self.x / scalar, self.y / scalar)

    def __floordiv__(self, scalar):
        return Point(self.x // scalar, self.y // scalar)

    def __str__(self):
        return '(' + str(self.x) + ',' + str(self.y) + ')'

    def roundToInt(self):
        return (int(round(self.x)), int(round(self.y)))

    def norm(self):
        return math.sqrt(self.x**2 + self.y**2)

#--------------------------------------------------------

class Align(Enum):
    CENTERED = 0
    LEFT = 1
    RIGHT = 2
    ABOVE = 3
    BELOW = 4

#--------------------------------------------------------

class AdvFigure:
    def __init__(self, key):
        self.key = key
        self.referencePoint = Point(0,0)
        self.visible = False
        self.strokeColor = (0,0,0)
        self.strokeThickness = 2

    def draw(self, mat, scaleFrom, scaleTo):
        pass

#--------------------------------------------------------

class AdvStateFigure(AdvFigure):
    def __init__(self, key, origin):
        super().__init__(key)
        self.accepting = False
        self.initial = False
        self.referencePoint = origin
        self.radius = 0.5

    def draw(self, mat, scaleFrom, scaleTo):
        # if not visible do nothing
        if not self.visible:
            return

        print('  Drawing state ' + self.key)

        # determine center and radius in image coordinates
        c = self.referencePoint / scaleFrom * scaleTo
        center = c.roundToInt()
        r = int(round(self.radius / scaleFrom * scaleTo))

        # draw state shape
        cv.circle(mat, center, r, self.strokeColor, self.strokeThickness)
        if self.accepting == True:
            r2 = int(round(0.8 * self.radius / scaleFrom * scaleTo))
            cv.circle(mat, center, r2, self.strokeColor, self.strokeThickness)
        if self.initial == True:
            pass # ...

        # draw label
        sz,_ = cv.getTextSize(self.key, cv.FONT_HERSHEY_SIMPLEX, 0.8, self.strokeThickness)
        c = c + Point(-sz[0]/2, sz[1]/2)
        center = c.roundToInt()
        cv.putText(mat, self.key, center, cv.FONT_HERSHEY_SIMPLEX, 0.8, self.strokeThickness)

#--------------------------------------------------------

class AdvTransitionFigure(AdvFigure):
    def __init__(self, key, label):
        super().__init__(key)
        self.label = label
        self.labelReferencePoint = Point(0,0)
        self.labelAlignment = Align.CENTERED
        self.arrowPoints = []

    def draw(self, mat, scaleFrom, scaleTo):
        # if not visible do nothing
        if not self.visible:
            return

        print('  Drawing transition ' + self.key)

        # convert arrow's points to image coordinates
        points = []
        for p in self.arrowPoints:
            p1 = p / scaleFrom * scaleTo
            points.append(p1.roundToInt())

        print(points)
        # draw the arrow, assuming there are at least 2 points
        for i, p in enumerate(points[:-2]):
            cv.line(mat, p, points[i+1], self.strokeColor, self.strokeThickness)
        cv.arrowedLine(mat, points[-2], points[-1], self.strokeColor, self.strokeThickness)

#--------------------------------------------------------

class AdvLoopTransitionFigure(AdvTransitionFigure):
    def __init__(self, key, label, p):
        super().__init__(key, label)

        # set arrow points
        p1 = p + Point(-0.2, -0.6)
        self.arrowPoints.append(p1)
        p1 = p1 + Point(-0.2, -0.3)
        self.arrowPoints.append(p1)
        p1 = p1 + Point(0.8, 0.0)
        self.arrowPoints.append(p1)
        p1 = p1 + Point(-0.2, 0.3)
        self.arrowPoints.append(p1)

        # set label reference point and alignment
        p1 = p1 + Point(0.2, -0.2)
        self.labelReferencePoint = p1
        self.labelAlignment = Align.LEFT;

#--------------------------------------------------------

class AdvLineTransitionFigure(AdvTransitionFigure):
    def __init__(self, key, label, p1, p2):
        super().__init__(key, label)

        # set arrow points
        p21 = p2 - p1
        d = p21 / p21.norm() * 0.7
        pa = p1 + d
        self.arrowPoints.append(pa)
        pb = p2 -d
        self.arrowPoints.append(pb)

        # set label reference point and alignment
        p = (pa + pb) / 2 + Point(0, -0.2)
        self.labelReferencePoint = p
        self.labelAlignment = Align.CENTERED;

#--------------------------------------------------------

class AdvAutomatonView:
    def __init__(self):
        self.name = ""
        self.figures = {}

    def addFigure(self, key, figure):
        self.figures[key] = figure

    def draw(self, mat, scaleFrom, scaleTo):
        for f in self.figures.values():
            f.draw(mat, scaleFrom, scaleTo)

#--------------------------------------------------------

# Animation code

# create an automaton view
av = AdvAutomatonView()

# place states
f = AdvStateFigure('A', Point(2.0, 1.0))
av.addFigure('A', f)
f = AdvStateFigure('B', Point(5.0, 1.0))
av.addFigure('B', f)

# transitions not explicitly shaped get default shapes
f = AdvLoopTransitionFigure('<A,A>', "'a','b','c'", Point(2.0, 1.0))
av.addFigure('<A,A>', f)
f = AdvLineTransitionFigure('<A,B>', "'a','b'", Point(2.0, 1.0), Point(5.0, 1.0))
av.addFigure('<A,B>', f)

# set A as the initial state
av.figures['A'].initial = True

# create the main window (animation support)
window = np.zeros((510, 510, 3), dtype="uint8")
window.fill(100)

# create a viewport (vp1)
vp1 = np.zeros((500, 500, 3), dtype="uint8")
vp1.fill(255)

# 1st image
print('----------------------------------------')
print('Drawing 1st image')
av.figures['A'].visible = True;
av.figures['B'].visible = True;
av.figures['B'].accepting = False;
av.draw(vp1, 1.0, 50)
np.copyto(window[10:,10:,:], vp1)
cv.imshow('Animation a1', window)
cv.waitKey(0)

# 2nd image
print('----------------------------------------')
print('Drawing 2nd image')
av.figures['<A,B>'].visible = True;
av.draw(vp1, 1.0, 50)
np.copyto(window[10:,10:,:], vp1)
cv.imshow('Animation a1', window)
cv.waitKey(0)

# 3rd image
print('----------------------------------------')
print('Drawing 3rd image')
av.figures['<A,A>'].visible = True;
av.draw(vp1, 1.0, 50)
np.copyto(window[10:,10:,:], vp1)
cv.imshow('Animation a1', window)
cv.waitKey(0)

# 4th image
print('----------------------------------------')
print('Drawing 4th image')
av.figures['B'].accepting = True;
av.draw(vp1, 1.0, 50)
np.copyto(window[10:,10:,:], vp1)
cv.imshow('Animation a1', window)
cv.waitKey(0)

print('----------------------------------------')
print('The end')
print('----------------------------------------')
