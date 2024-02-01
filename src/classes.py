from dataclasses import dataclass, field, astuple, asdict
import dataclasses
import sys
from typing import Dict, List, Set
import cv2 as cv
import numpy as np
import math
from enum import Enum


@dataclass(frozen=True)
class Point:
    x: int = 0
    y: int = 0

    @classmethod
    def fromPolar(cls, angle, norm):
        assert norm >= 0
        angle_rad = math.radians(angle)
        x = norm * math.cos(angle_rad)
        y = norm * math.sin(angle_rad)
        return cls(x, y)
        
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
    
    def getTuple(self) -> tuple:
        return (self.x, self.y)
    

class Align(Enum):
    CENTERED = 0
    LEFT = 1
    RIGHT = 2
    ABOVE = 3
    BELOW = 4

class AdvFigure:
    def __init__(self, key: str):
        self.key = key
        self.referencePoint = Point(0,0)
        self.visible = False
        self.strokeColor = (0,0,0)
        self.strokeThickness = 2

    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
        pass

class AdvStateFigure(AdvFigure):
    def __init__(self, key, origin):
        super().__init__(key)
        self.accepting = False
        self.initial = False
        self.referencePoint = origin
        self.radius = 0.5
        self.highlighted = False
        self.move_down = Point(0, 3)
    
    def setPoint(self, point):
        self.referencePoint = point + self.move_down

    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
        # if not visible do nothing
        if not self.visible:
            return

        print('  Drawing state ' + self.key)

        # determine center and radius in image coordinates
        c = self.referencePoint / scaleFrom * scaleTo
        center = c.roundToInt()
        r = int(round(self.radius / scaleFrom * scaleTo))

        # draw state shape
        if self.highlighted == True:
            cv.circle(mat, center, r, (0, 255, 255), -1)
        else:
            cv.circle(mat, center, r, (255, 255, 255), -1)
        cv.circle(mat, center, r, self.strokeColor, self.strokeThickness)
        if self.accepting == True:
            r2 = int(round(0.8 * self.radius / scaleFrom * scaleTo))
            cv.circle(mat, center, r2, self.strokeColor, self.strokeThickness)
        if self.initial == True:
            pt1 = center[0] -r - 50, center[1]
            pt2 = center[0] -r - 10, center[1] 
            print(pt1, pt2)
            cv.arrowedLine(mat, pt1, pt2, self.strokeColor, self.strokeThickness, tipLength=0.2)

        # draw label
        sz,_ = cv.getTextSize(self.key, cv.FONT_HERSHEY_SIMPLEX, 0.8, self.strokeThickness)
        c = c + Point(-sz[0]/2, sz[1]/2)
        center = c.roundToInt()
        cv.putText(mat, self.key, center, cv.FONT_HERSHEY_SIMPLEX, 0.8, self.strokeThickness)

#--------------------------------------------------------

class AdvTransitionFigure(AdvFigure):
    def __init__(self, key, label):
        super().__init__(key)
        if len(label) == 0:
            self.label = ''
        else:
            lb = list(label)
            lb.sort()
            self.label = (str(lb))[1:-1]
        self.labelReferencePoint = Point(0,0)
        self.labelAlignment = [Align.CENTERED]
        self.arrowPoints = []
        self.isCurve = False
        self.slopes = None
    
    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
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
        if self.isCurve == False:
            for i, p in enumerate(points[:-2]):
                cv.line(mat, p, points[i+1], self.strokeColor, self.strokeThickness)
            cv.arrowedLine(mat, np.int32(points[-2]), np.int32(points[-1]), self.strokeColor, self.strokeThickness)
        else:
            x = np.array([point[0] for point in points])
            y = np.array([point[1] for point in points])
            A = np.array([[points[0][0]**2, points[0][0], 1],
                            [points[1][0]**2, points[1][0], 1],
                            [points[2][0]**2, points[2][0], 1]])

            coefficients = np.linalg.solve(A, y)

            curve_x = np.linspace(min(x), max(x), 100)
            curve_y = coefficients[0] * curve_x**2 + coefficients[1] * curve_x + coefficients[2]
            
            points = list(zip(curve_x, curve_y))
            cv.polylines(mat, np.int32([points]), False, self.strokeColor, self.strokeThickness)
            cv.arrowedLine(mat, np.int32(points[5]), np.int32(points[0]), self.strokeColor, self.strokeThickness, tipLength=0.9)
            px = points[len(points) // 2][0] * scaleFrom / scaleTo
            py = points[len(points) // 2][1] * scaleFrom / scaleTo
            self.labelReferencePoint = Point(px, py)
            self.labelReferencePoint += Point(0, -0.1)
            for a in self.labelAlignment:
                if a == Align.ABOVE:
                    self.labelReferencePoint += Point(0, -0.1)
                elif a == Align.BELOW:
                    self.labelReferencePoint += Point(0, 0.3)
                elif a == Align.LEFT:
                    self.labelReferencePoint += Point(-0.09 * len(self.label), 0)
                elif a == Align.RIGHT:
                    self.labelReferencePoint += Point(0.02 * len(self.label), 0)

        label_origin = self.labelReferencePoint / scaleFrom * scaleTo
        if isinstance(self, AdvLineTransitionFigure):
            sz,_ = cv.getTextSize(self.label, cv.FONT_HERSHEY_SIMPLEX, 0.5, self.strokeThickness)
            label_origin = label_origin + Point(-sz[0]/2, sz[1]/2)
        cv.putText(mat, self.label, label_origin.roundToInt(), cv.FONT_HERSHEY_SIMPLEX, 0.5, self.strokeThickness)

#--------------------------------------------------------

class AdvLoopTransitionFigure(AdvTransitionFigure):
    def __init__(self, key, label, state: AdvStateFigure):
        super().__init__(key, label)
        self.state = state
        self.labelAlignment = [Align.RIGHT]

    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
        # set arrow points
        p1 = self.state.referencePoint + Point(-0.2, -0.6)
        self.arrowPoints.append(p1)
        p1 = p1 + Point(-0.5, -0.3)
        p1 = p1 + Point(0.7, -0.2)
        self.arrowPoints.append(p1)
        p1 = p1 + Point(0.7, 0.0)
        # self.arrowPoints.append(p1)
        p1 = p1 + Point(-0.5, 0.5)
        self.arrowPoints.append(p1)

        # set label reference point and alignment
        self.labelReferencePoint = p1
        for a in self.labelAlignment:
            if a == Align.CENTERED:
                self.labelReferencePoint += Point(-0.6, -0.5)
            elif a == Align.ABOVE:
                self.labelReferencePoint += Point(-0.6, -0.7)
            elif a == Align.BELOW:
                self.labelReferencePoint += Point(-0.6, 0.9)
            elif a == Align.LEFT:
                self.labelReferencePoint += Point(-1.2, -0.5)
            elif a == Align.RIGHT:
                self.labelReferencePoint += Point(0.2, -0.2)

        self.isCurve = True
        super().draw(mat, scaleFrom, scaleTo)

#--------------------------------------------------------

class AdvLineTransitionFigure(AdvTransitionFigure):
    def __init__(self, key, label, state1: AdvStateFigure, state2: AdvStateFigure):
        super().__init__(key, label)
        self.state1 = state1
        self.state2 = state2
        self.labelAlignment = [Align.CENTERED]

    def curvePoints(self, p1, pm, p2):
        self.slopes = list()
        self.isCurve = True
        if isinstance(p1, tuple) == False:
            p1 = (p1, None)
        if isinstance(pm, tuple) == False:
            pm = (pm, None)
        if isinstance(p2, tuple) == False:
            p2 = (p2, None)
        self.slopes.append(p1[1])
        self.slopes.append(pm[1])
        self.slopes.append(p2[1])
        p1 = p1[0] - Point(0, (p1[0].y - self.state1.referencePoint.getTuple()[1]) * 2)
        pm = pm[0] - Point(0, (pm[0].y - self.state1.referencePoint.getTuple()[1]) * 2)
        p2 = p2[0] - Point(0, (p2[0].y - self.state1.referencePoint.getTuple()[1]) * 2)
        self.arrowPoints = [p1, pm, p2]

    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
        if self.isCurve == False:
            p21 = self.state2.referencePoint - self.state1.referencePoint
            d = p21 / p21.norm() * 0.7
            pa = self.state1.referencePoint + d
            self.arrowPoints.append(pa)
            pb = self.state2.referencePoint - d
            self.arrowPoints.append(pb)
        else:
            pa = self.arrowPoints[0]
            pb = self.arrowPoints[-1]
        
        p = (pa + pb) / 2 + Point(0, -0.2)
        for a in self.labelAlignment:
            if a == Align.CENTERED:
                self.labelReferencePoint = p
            elif a == Align.ABOVE:
                self.labelReferencePoint = p + Point(0, -0.7)
            elif a == Align.BELOW:
                self.labelReferencePoint = p + Point(0, 0.6)
            elif a == Align.LEFT:
                if self.labelReferencePoint == Point(0, 0):
                    self.labelReferencePoint = p
                if pa.getTuple()[0] < pb.getTuple()[0]:
                    self.labelReferencePoint -= ((pb - p) / 1.3) - Point(-0.06 * len(self.label), 0)
                else:
                    self.labelReferencePoint -= (pa - p) / 1.3 - Point(-0.06 * len(self.label), 0)
            elif a == Align.RIGHT:
                if self.labelReferencePoint == Point(0, 0):
                    self.labelReferencePoint = p
                if pa.getTuple()[0] < pb.getTuple()[0]:
                    self.labelReferencePoint += (pb - p) / 1.3 - Point(0.06 * len(self.label), 0)
                else:
                    self.labelReferencePoint += (pa - p) / 1.3 - Point(0.06 * len(self.label), 0)

        super().draw(mat, scaleFrom, scaleTo)

@dataclass
class Grid:
    key: str = ""
    size: tuple = (0,0)

    def __post_init__(self):
        self.step: float = 0.0
        self.margin: float = 0.0
        self.color: str = "gray"
        self.line: str = "solid"

    def getColor(self):
        if 'gray' in self.color:
            return (128,128,128)
        if 'red' in self.color:
            return (0,0,255)
        if 'green' in self.color:
            return (0,255,0)
        if 'blue' in self.color:
            return (255,0,0)
        if 'yellow' in self.color:
            return (0,255,255)
        if 'purple' in self.color:
            return (255,0,255)
        if 'orange' in self.color:
            return (0,165,255)
        if 'brown' in self.color:
            return (19,69,139)
        if 'black' in self.color:
            return (0,0,0)
        if 'white' in self.color:
            return (255,255,255)
        if 'pink' in self.color:
            return (147,20,255)
        else:
            print('Color not found')
            return (128,128,128)


    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
        width = int(round(self.size[0] / scaleFrom * scaleTo))
        height = int(round(self.size[1] / scaleFrom * scaleTo))
        step = int(round(self.step / scaleFrom * scaleTo))
        margin = int(round(self.margin / scaleFrom * scaleTo))
        color = self.getColor()
        if (self.line == 'solid') :
            for i in range(margin, height+margin+1, step):
                cv.line(mat, (margin,i), (width+margin,i), color, thickness=1)
            for j in range(margin, width+margin+1, step):
                cv.line(mat, (j,margin), (j,height+margin), color, thickness=1)
        else:
            for i in range(margin, height+margin+1, step):
                dashedLine((margin,i), (width+margin,i), self.line, color)
            for j in range(margin, width+margin+1, step):
                dashedLine((j,margin), (j,height+margin), self.line, color)

        def dashedLine(pt1,pt2,line='dashed',color='gray'):
            dist =((pt1[0]-pt2[0])**2+(pt1[1]-pt2[1])**2)**.5
            pts= []
            for i in np.arange(0,dist+1,5):
                r=i/dist
                x=int((pt1[0]*(1-r)+pt2[0]*r)+.5)
                y=int((pt1[1]*(1-r)+pt2[1]*r)+.5)
                p = (x,y)
                pts.append(p)

            if line=='dotted':
                for p in pts:
                    cv.line(mat, p, p, color, 1)
            else:
                s=pts[0]
                e=pts[0]
                i=0
                for p in pts:
                    s=e
                    e=p
                    if i%2==1:
                        cv.line(mat,s,e,color,1)
                    i+=1
@dataclass
class Automaton:
    alphabet: Set[str] = field(default_factory=set)
    type_: str = ""
    states: Dict[str, AdvStateFigure] = field(default_factory=dict)
    transitions: Dict[str, AdvTransitionFigure] = field(default_factory=dict)
    initial_count: int = 0

    def __post_init__(self):
        # verify if alphabet is valid (only ASCII characters)
        for char in self.alphabet:
            assert ord(char) <= 127, f"Invalid character in the alphabet: {char}"
        # verify if _type is NFA or DFA or complete DFA
        assert self.type_ in ['NFA', 'DFA', 'complete DFA'], f"Invalid automaton type: {self.type_}"

    def addStates(self, states: set("str")):
        for state in states:
            self.addState(state)

    def addState(self, state: str, label: str = None, labelValue: str = None):
        if label == 'initial':
            assert self.initial_count == 0, "There must be one and only one initial state"
        self.states[state] = AdvStateFigure(state, None)
        if label != None:
            self.stateLabel(state, label, labelValue)
    
    def getStates(self, states: set("str")):
        return self.states

    def getState(self, state: str):
        return self.states[state]
    
    def stateLabel(self, state: str, label: str, labelValue: str):
        if label == 'initial':
            assert self.initial_count == 0, "There must be one and only one initial state"
            self.initial_count += 1
            if labelValue == 'true':
                self.states[state].initial = True
            else:
                self.states[state].initial = False
        elif label == 'accepting':
            if labelValue == 'true':
                self.states[state].accepting = True
            else:
                self.states[state].accepting = False
        elif label == 'highlighted':
            if labelValue == 'true':
                self.states[state].highlighted = True
            else:
                self.states[state].highlighted = False
                    
    def addTransition(self, fromState: str, toState: str, symbols: Set[str]):
        """ Add a transition to the list of transitions """
        key = "<" + fromState + "," + toState + ">"
        if fromState == toState:
            state = self.states[fromState]
            self.transitions[key] = (AdvLoopTransitionFigure(key, symbols, state))
        else:
            state1 = self.states[fromState]
            state2 = self.states[toState]
            self.transitions[key] = (AdvLineTransitionFigure(key, symbols, state1, state2))

    def getNextState(self, fromState: str, symbol: str):
        for transition in self.transitions.values():
            if transition.key.startswith('<' + fromState):
                if symbol in transition.label.replace('\'', '').replace(' ', '').split(','):
                    s = transition.key.split(",")[1][:-1]
                    return self.states[s]
        print("Couldn't find next state for symbol", symbol, file=sys.stderr)
        sys.exit(1)


@dataclass
class View:
    automaton: Automaton
    points: Dict[str, Point] = field(default_factory=dict)

    def __post_init__(self):
        self.grids = dict()
    
    def placeState(self, state, point): # ? Função igual à do adv que recebe States e coords ou transicoes/labels/ponto(coords)
        """ Place a state at a given point """
        self.automaton.states[state].setPoint(point)
    
    def addPoints(self, points: tuple("str")):
        """ Add points to the list of points """
        for name in points:
            self.points[name] = Point()

    def addPoint(self, name):
        """ Add a point to the list of points """
        self.points[name] = Point()

    def addPointNewCartesian(self, name, x, y):
        """ Add a point to the list of points """
        self.points[name] = Point(x, y)

    def addPointNewPolar(self, name, angle, norm):
        """ Add a point to the list of points """
        self.points[name] = Point.fromPolar(angle, norm)
     
    def pointFromState(self, name, state):
        """ Place a point at a given state """
        for a_state in self.automaton.states:
            if a_state.key == state:
                self.points[name] = a_state.referencePoint
                break   
    
    def redefineTransitionArrows(self, transitionKey: str, p1: tuple, pm: tuple, p2: tuple):
        """ Redefine a transition between two states """
        self.automaton.transitions[transitionKey].curvePoints(p1, pm, p2)

    def placeTransitionLabel(self, transitionKey, alignment, point):
        """ Place a transition label """
        if isinstance(alignment, list) == False:
            alignment = [alignment]
        self.automaton.transitions[transitionKey].labelAlignment = alignment
        self.automaton.transitions[transitionKey].labelReferencePoint = point

    def changeLabelAlignment(self, transitionKey, alignment):
        """ Change a transition label alignment """
        if isinstance(alignment, list) == False:
            alignment = [alignment]
        self.automaton.transitions[transitionKey].labelAlignment = alignment

    def addGrid(self, key: str, size: tuple):
        """ Add a grid to the view """
        self.grids[key] = Grid(key, size)

    def draw(self, mat, scaleFrom = 1.0, scaleTo = 50):
        for state in self.automaton.states:
            state.draw(mat, scaleFrom)
        for transition in self.automaton.transitions.values():
            transition.draw(mat, scaleFrom, scaleTo)

    
@dataclass
class Viewport:
    anim_key: str
    key: str
    view: View
    start_coords: tuple
    size: tuple
    vp: None = None
    window: None = None

    def __post_init__(self):
        # verify if size is valid
        assert self.size.__len__() == 2, "Invalid size"
        assert self.size[0] > 0 and self.size[1] > 0, "Invalid size"
        # verify if initial point is valid
        assert self.start_coords.__len__() == 2, "Invalid start point"
        assert self.start_coords[0] >= 0 and self.start_coords[1] >= 0, "Invalid start point"
        self.window = np.zeros((self.size[0] + self.start_coords[0], \
                                self.size[1] + self.start_coords[1], 3), dtype="uint8")
        self.window.fill(100)
        self.vp = np.zeros((self.size[0], self.size[1], 3), dtype="uint8")
        self.vp.fill(255)
        

    def showState(self, state, label = None, labelValue = None):
        """ Show a state """
        self.view.automaton.states[state].visible = True
        if label != None:
            if label == 'highlighted':
                for s in self.view.automaton.states.values():
                    if s.highlighted == True:
                        s.highlighted = False
                        s.draw(self.vp)
                        break
            self.view.automaton.stateLabel(state, label, labelValue)
        self.view.automaton.states[state].draw(self.vp)
        np.copyto(self.window[self.start_coords[0]:, self.start_coords[1]:,:], self.vp)
        cv.imshow('Animation ' + self.anim_key, self.window)
    
    def showTransitions(self, transitions: tuple):
        """ Show a list of transitions """
        for transitionKey in transitions:
            self.view.automaton.transitions[transitionKey].visible = True
            self.view.automaton.transitions[transitionKey].draw(self.vp)
        np.copyto(self.window[self.start_coords[0]:, self.start_coords[1]:,:], self.vp)
        cv.imshow('Animation ' + self.anim_key, self.window)

    def showTransition(self, transitionKey):
        """ Show a transition """
        self.view.automaton.transitions[transitionKey].visible = True
        self.view.automaton.transitions[transitionKey].draw(self.vp)
        np.copyto(self.window[self.start_coords[0]:, self.start_coords[1]:,:], self.vp)
        cv.imshow('Animation ' + self.anim_key, self.window)

    def showGrid(self, key: str):
        self.view.grids[key].draw(self.vp)

    def show(self):
        for state in self.view.automaton.states.values():
            state.visible = True
            state.draw(self.vp)
            np.copyto(self.window[self.start_coords[0]:, self.start_coords[1]:,:], self.vp)
            cv.imshow('Animation ' + self.anim_key, self.window)
        for transition in self.view.automaton.transitions.values():
            transition.visible = True
            transition.draw(self.vp)
            np.copyto(self.window[self.start_coords[0]:, self.start_coords[1]:,:], self.vp)
            cv.imshow('Animation ' + self.anim_key, self.window)
        self.pause()

    def pause(self):
        cv.waitKey(0)


@dataclass
class Animation:
    key: str = ""
    
    def addViewport(self, vp_key: str, view: View, start: tuple, size: tuple):
        self.viewport = Viewport(self.key, vp_key, view, start, size)


def main():
    # # ex01
    # alphabet = {'a', 'b', 'c'}
    # a1 = Automaton(alphabet, 'NFA')
    # a1.addState('A')
    # a1.addState('B')

    # a1.stateLabel('A', 'initial', 'true')
    # a1.stateLabel('B', 'accepting', 'true')

    # a1.addTransition('A', 'B', {'a', 'b'})
    # a1.addTransition('A', 'A', {'a', 'b', 'c'})

    # v1 = View(a1)
    # v1.placeState('A', Point(2,1))
    # v1.placeState('B', Point(5,1))

    # # Por agora fica assim porque Animation parece-me ser uma função que depois é chamada com o play
    # def m1():
    #     m1 = Animation('m1')
    #     m1.addViewport('vp1', v1, (10, 10), (500, 500))
    #     m1.viewport.showState('A')
    #     m1.viewport.showState('B', 'accepting', 'false')
    #     m1.viewport.pause()
    #     m1.viewport.showTransition('<A,B>')
    #     m1.viewport.pause()
    #     m1.viewport.showTransition('<A,A>')
    #     m1.viewport.pause()
    #     m1.viewport.showState('B', 'accepting', 'true')
    #     m1.viewport.pause()

    # m1()


    # # ex02
    # alphabet = {'a', 'b', 'c'}
    # a2 = Automaton(alphabet, 'DFA')
    # a2.addState('A')
    # a2.addState('B')

    # a2.stateLabel('A', 'initial', 'true')
    # a2.stateLabel('B', 'accepting', 'true')

    # a2.addTransition('A','B', {'a','b'})
    # a2.addTransition('A','A', {'c'})
    # a2.addTransition('B','A', {'c'})
    # a2.addTransition('B','B', {'a','b'})
    
    # v2 = View(a2)
    # v2.placeState('A', Point(2,1))
    # v2.placeState('B', Point(5,1))

    # v2.addPoint('p1')
    # v2.points['p1'] = a2.states['B'].referencePoint

    # v2.addPoint('x1')
    # v2.points['x1'] = Point.fromPolar(200, 0.6)

    # v2.points['p1'] = v2.points['p1'] + v2.points['x1']

    # v2.addPoint('p2')
    # v2.points['p2'] = v2.automaton.states['A'].referencePoint + Point.fromPolar(-20, 0.6)

    # v2.addPoint('pm')

    # temp_p = v2.points['p1'] + v2.points['p2']
    # temp_p = temp_p / 2
    # v2.points['pm'] = temp_p + Point(0, -0.2) # -0.2 para fazer sentido com os angulos

    # # v2.points['pm'] = (v2.points['p1'] + v2.points['p2']) / 2 + Point(0, -0.2) # -0.2 para fazer sentido com os angulos

    # v2.redefineTransitionArrows('<B,A>', v2.points['p1'], v2.points['pm'], v2.points['p2'])
    # v2.placeTransitionLabel('<B,A>', [Align.ABOVE] , v2.points['pm']) # pa não existe, deve ser pm

    # def m2():
    #     m2 = Animation('m2')
    #     m2.addViewport('vp2', v2, (10, 10), (500, 500))
    #     m2.viewport.showState('A')
    #     m2.viewport.pause()
    #     m2.viewport.showTransition('<A,A>')
    #     m2.viewport.pause()
    #     m2.viewport.showState('B')
    #     m2.viewport.showTransition('<A,B>')
    #     m2.viewport.pause()
    #     m2.viewport.showTransition('<B,B>')
    #     m2.viewport.pause()
    #     m2.viewport.showTransition('<B,A>')
    #     m2.viewport.pause()

    # m2()

    # # ex03
    # alphabet = {'a', 'b', 'c'}
    # a3 = Automaton(alphabet, 'complete DFA')
    # a3.addStates({'A', 'B', 'C', 'D', 'E'})

    # a3.stateLabel('A', 'initial', 'true')
    # a3.stateLabel('B', 'accepting', 'true')

    # for state in {'A', 'B', 'D'}:
    #     a3.stateLabel(state, 'accepting', 'true')                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     

    # a3.addTransition('A','B', {'a'})
    # a3.addTransition('B','C', {'b'})
    # a3.addTransition('C','D', {'c'})
    # a3.addTransition('C','E', {'a','b'})
    # a3.addTransition('E','E', {'a','b', 'c'})
    # a3.addTransition('B','B', {'a'})
    # a3.addTransition('B','A', {'c'})
    # a3.addTransition('A','A', {'c'})
    # a3.addTransition('D','D', {'c'})
    # a3.addTransition('D','B', {'a'})
    # a3.addTransition('D','E', {'b'})
    # a3.addTransition('A','E', {'b'})
    
    # v3 = View(a3)
    # v3.addGrid('g3', (21, 10))
    # v3.grids['g3'].step = 0.5
    # v3.grids['g3'].margin = 0.25
    # v3.grids['g3'].color = 'gray'
    # v3.grids['g3'].line = 'solid'

    # v3.placeState('A', Point(2,1))
    # v3.placeState('B', Point(5,1))
    # v3.placeState('C', Point(7,1))
    # v3.placeState('D', Point(10,1))
    # v3.placeState('E', Point(4.5,4))

    # v3.addPoints(('p1', 'p2', 'pm'))
    # v3.points['p1'] = v3.automaton.states['B'].referencePoint + Point.fromPolar(160,0.7)
    # v3.points['p2'] = v3.automaton.states['A'].referencePoint + Point.fromPolar(20,0.7)
    # temp_p = v3.automaton.states['A'].referencePoint + v3.automaton.states['B'].referencePoint
    # temp_p = temp_p / 2
    # v3.points['pm'] = temp_p + Point(0, 0.5)

    # v3.redefineTransitionArrows('<B,A>', v3.points['p1'], v3.points['pm'], v3.points['p2'])
    # v3.placeTransitionLabel('<B,A>', [Align.ABOVE] , v3.points['pm'])
    
    # v3.points['p1'] = v3.automaton.states['D'].referencePoint + Point.fromPolar(160,0.7)
    # v3.points['p2'] = v3.automaton.states['B'].referencePoint + Point.fromPolar(20,0.7)
    # temp_p = v3.automaton.states['D'].referencePoint + v3.automaton.states['B'].referencePoint
    # temp_p = temp_p / 2
    # v3.points['pm'] = temp_p + Point(0, 1.0)

    # v3.redefineTransitionArrows('<D,B>', v3.points['p1'], v3.points['pm'], v3.points['p2'])
    # v3.placeTransitionLabel('<D,B>', [Align.ABOVE] , v3.points['pm'])

    # v3.changeLabelAlignment('<A,E>', [Align.BELOW, Align.LEFT])
    # v3.changeLabelAlignment('<D,E>', [Align.BELOW, Align.RIGHT])
    # v3.changeLabelAlignment('<C,E>', [Align.RIGHT])
    # v3.changeLabelAlignment('<E,E>', [Align.LEFT])


    # def m3():
    #     m3 = Animation('m3')
    #     m3.addViewport('vp3', v3, (10, 10), (500, 600))
    #     m3.viewport.showGrid('g3')
    #     m3.viewport.pause()
    #     for i in {'A', 'B', 'D'}:
    #         m3.viewport.showState(i, 'accepting', 'false')
    #     m3.viewport.showState('C')
    #     m3.viewport.showTransitions(('<A,B>','<B,C>', '<C,D>'))
    #     m3.viewport.pause()
    #     m3.viewport.showState('E')
    #     m3.viewport.showTransitions(('<C,E>', '<E,E>'))
    #     m3.viewport.pause()
    #     m3.viewport.showTransitions(('<B,B>', '<B,A>'))
    #     m3.viewport.pause()
    #     m3.viewport.showTransitions(('<A,A>', '<A,E>', '<D,D>', '<D,E>', '<D,B>'))
    #     m3.viewport.pause()
    #     for i in {'A', 'B', 'D'}:
    #         m3.viewport.showState(i, 'accepting', 'true')
    #     m3.viewport.pause()
    
    # m3()

    # ex04
    alphabet = {'a', 'b', 'c'}
    a4 = Automaton(alphabet, 'complete DFA')
    a4.addStates({'A', 'B', 'C', 'D', 'E'})

    a4.stateLabel('A', 'initial', 'true')
    a4.stateLabel('B', 'accepting', 'true')

    for state in {'A', 'B', 'D'}:
        a4.stateLabel(state, 'accepting', 'true')

    a4.addTransition('A','B', {'a'})
    a4.addTransition('B','C', {'b'})
    a4.addTransition('C','D', {'c'})
    a4.addTransition('C','E', {'a','b'})
    a4.addTransition('E','E', {'a','b','c'})
    a4.addTransition('B','B', {'a'})
    a4.addTransition('B','A', {'c'})
    a4.addTransition('A','A', {'c'})
    a4.addTransition('D','A', {'c'})
    a4.addTransition('D','B', {'a'})
    a4.addTransition('D','E', {'b'})
    a4.addTransition('A','E', {'b'})
    
    v4 = View(a4)

    v4.placeState('A', Point(2,1))
    v4.placeState('B', Point(5,1))
    v4.placeState('C', Point(7,1))
    v4.placeState('D', Point(10,1))
    v4.placeState('E', Point(5,4))

    v4.addPoints(('p1', 'p2', 'pm'))
    v4.points['p1'] = v4.automaton.states['B'].referencePoint + Point.fromPolar(215, 0.7)
    v4.points['p2'] = v4.automaton.states['A'].referencePoint + Point.fromPolar(-35, 0.7)
    temp_p = v4.automaton.states['A'].referencePoint + v4.automaton.states['B'].referencePoint
    temp_p = temp_p / 2
    v4.points['pm'] = temp_p + Point(0, -0.5)
    v4.redefineTransitionArrows('<B,A>', (v4.points['p1'], 235), (v4.points['pm'], 0), (v4.points['p2'], 45))
    v4.placeTransitionLabel('<B,A>', [Align.BELOW] , v4.points['pm'])
    
    v4.points['p1'] = v4.automaton.states['D'].referencePoint + Point.fromPolar(150, 0.7)
    v4.points['p2'] = v4.automaton.states['B'].referencePoint + Point.fromPolar(30, 0.7)
    temp_p = v4.automaton.states['D'].referencePoint + v4.automaton.states['B'].referencePoint
    temp_p = temp_p / 2
    v4.points['pm'] = temp_p + Point(0, 1.0)
    v4.redefineTransitionArrows('<D,B>', (v4.points['p1'], 150), (v4.points['pm'], 0), (v4.points['p2'], 30))
    v4.placeTransitionLabel('<D,B>', [Align.ABOVE] , v4.points['pm'])

    v4.points['p1'] = v4.automaton.states['D'].referencePoint + Point.fromPolar(120, 0.7)
    v4.points['p2'] = v4.automaton.states['A'].referencePoint + Point.fromPolar(60, 0.7)
    temp_p = v4.automaton.states['D'].referencePoint + v4.automaton.states['A'].referencePoint
    temp_p = temp_p / 2
    v4.points['pm'] = temp_p + Point(0, 1.5)
    v4.redefineTransitionArrows('<D,A>', (v4.points['p1'], 120), (v4.points['pm'], 0), (v4.points['p2'], 60))
    v4.placeTransitionLabel('<D,A>', Align.ABOVE , v4.points['pm'])

    v4.changeLabelAlignment('<A,E>', [Align.BELOW, Align.LEFT])
    v4.changeLabelAlignment('<D,E>', [Align.BELOW, Align.RIGHT])
    v4.changeLabelAlignment('<C,E>', [Align.RIGHT])
    v4.changeLabelAlignment('<E,E>', [Align.LEFT])


    def m4():
        m4 = Animation('m4')
        m4.addViewport('vp4', v4, (10, 10), (500, 600))
        m4.viewport.show()
        
        word = input('Insira uma palavra: ')

        cs = m4.viewport.view.automaton.getState('A')
        m4.viewport.showState(cs.key, 'highlighted', 'true')
        m4.viewport.pause()
        for l in word:
            cs = m4.viewport.view.automaton.getNextState(cs.key, l)
            m4.viewport.showState(cs.key, 'highlighted', 'true')
            m4.viewport.pause()
    
    m4()




if __name__ == '__main__':
    main()