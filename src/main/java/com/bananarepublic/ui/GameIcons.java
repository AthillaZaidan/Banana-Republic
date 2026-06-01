package com.bananarepublic.ui;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class GameIcons {

    private GameIcons() {}

    public static Group dice() {
        Group g = new Group();
        Rectangle face = new Rectangle(2, 2, 18, 18);
        face.setArcWidth(4); face.setArcHeight(4);
        face.setFill(Color.WHITE);
        face.setStroke(Color.web("#333333"));
        face.setStrokeWidth(1.5);
        g.getChildren().add(face);
        int[][] dots = {{6,6},{15,6},{11,11},{6,15},{15,15}};
        for (int[] d : dots) {
            Circle dot = new Circle(d[0], d[1], 1.8, Color.web("#222222"));
            g.getChildren().add(dot);
        }
        return g;
    }

    public static Group scoreboard() {
        Group g = new Group();
        Rectangle body = new Rectangle(2, 4, 18, 16);
        body.setArcWidth(3); body.setArcHeight(3);
        body.setFill(Color.web("#f5e8c0"));
        body.setStroke(Color.web("#7a5a20"));
        body.setStrokeWidth(1.5);
        g.getChildren().add(body);
        for (int i = 0; i < 3; i++) {
            double y = 9 + i * 4;
            double barW = 9 - i * 2;
            Rectangle bar = new Rectangle(6, y, barW, 2.5);
            bar.setFill(Color.web("#c8901a"));
            bar.setArcWidth(1); bar.setArcHeight(1);
            g.getChildren().add(bar);
            Line tick = new Line(4, y + 1.2, 5.5, y + 1.2);
            tick.setStroke(Color.web("#7a5a20"));
            tick.setStrokeWidth(1.2);
            g.getChildren().add(tick);
        }
        return g;
    }

    public static Group trade() {
        Group g = new Group();
        // arrow right-up
        Line r = new Line(3, 14, 18, 14);
        r.setStroke(Color.web("#2a6ab5")); r.setStrokeWidth(2); r.setStrokeLineCap(StrokeLineCap.ROUND);
        Polygon arrowR = new Polygon(16, 11, 20, 14, 16, 17);
        arrowR.setFill(Color.web("#2a6ab5"));
        // arrow left-down
        Line l = new Line(18, 8, 3, 8);
        l.setStroke(Color.web("#c8901a")); l.setStrokeWidth(2); l.setStrokeLineCap(StrokeLineCap.ROUND);
        Polygon arrowL = new Polygon(5, 5, 1, 8, 5, 11);
        arrowL.setFill(Color.web("#c8901a"));
        g.getChildren().addAll(r, arrowR, l, arrowL);
        return g;
    }

    public static Group save() {
        Group g = new Group();
        Rectangle outer = new Rectangle(2, 2, 18, 18);
        outer.setArcWidth(2); outer.setArcHeight(2);
        outer.setFill(Color.web("#3a7abf"));
        outer.setStroke(Color.web("#1b4778")); outer.setStrokeWidth(1.5);
        g.getChildren().add(outer);
        Rectangle slot = new Rectangle(6, 2, 9, 7);
        slot.setFill(Color.web("#1b4778"));
        g.getChildren().add(slot);
        Rectangle notch = new Rectangle(10, 3, 2, 5);
        notch.setFill(Color.web("#89c4f4"));
        g.getChildren().add(notch);
        Rectangle disk = new Rectangle(4, 12, 14, 7);
        disk.setArcWidth(2); disk.setArcHeight(2);
        disk.setFill(Color.web("#1b4778"));
        g.getChildren().add(disk);
        Rectangle diskInner = new Rectangle(6, 14, 10, 3);
        diskInner.setFill(Color.web("#5aaae8"));
        g.getChildren().add(diskInner);
        return g;
    }

    public static Group plugin() {
        Group g = new Group();
        Rectangle body = new Rectangle(4, 6, 14, 12);
        body.setArcWidth(3); body.setArcHeight(3);
        body.setFill(Color.web("#6a3ab2"));
        body.setStroke(Color.web("#3a1a7a")); body.setStrokeWidth(1.5);
        g.getChildren().add(body);
        Line pin1 = new Line(8, 18, 8, 22); pin1.setStroke(Color.web("#3a1a7a")); pin1.setStrokeWidth(2);
        Line pin2 = new Line(14, 18, 14, 22); pin2.setStroke(Color.web("#3a1a7a")); pin2.setStrokeWidth(2);
        Line prong1 = new Line(6, 4, 6, 6); prong1.setStroke(Color.web("#3a1a7a")); prong1.setStrokeWidth(2);
        Line prong2 = new Line(16, 4, 16, 6); prong2.setStroke(Color.web("#3a1a7a")); prong2.setStrokeWidth(2);
        Circle eye1 = new Circle(9, 11, 2, Color.web("#c8a0ff"));
        Circle eye2 = new Circle(14, 11, 2, Color.web("#c8a0ff"));
        g.getChildren().addAll(pin1, pin2, prong1, prong2, eye1, eye2);
        return g;
    }

    public static Group anchor() {
        Group g = new Group();
        Circle ring = new Circle(11, 5, 3.5);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#1b4778")); ring.setStrokeWidth(2);
        g.getChildren().add(ring);
        Line shaft = new Line(11, 8, 11, 20);
        shaft.setStroke(Color.web("#1b4778")); shaft.setStrokeWidth(2.5); shaft.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(shaft);
        Line bar = new Line(5, 11, 17, 11);
        bar.setStroke(Color.web("#1b4778")); bar.setStrokeWidth(2);
        g.getChildren().add(bar);
        Arc arm = new Arc(11, 20, 5, 5, 0, 180);
        arm.setType(ArcType.OPEN);
        arm.setFill(Color.TRANSPARENT);
        arm.setStroke(Color.web("#1b4778")); arm.setStrokeWidth(2);
        g.getChildren().add(arm);
        Circle tipL = new Circle(6, 20, 1.5, Color.web("#1b4778"));
        Circle tipR = new Circle(16, 20, 1.5, Color.web("#1b4778"));
        g.getChildren().addAll(tipL, tipR);
        return g;
    }

    public static Group bot() {
        Group g = new Group();
        Rectangle head = new Rectangle(5, 3, 13, 10);
        head.setArcWidth(3); head.setArcHeight(3);
        head.setFill(Color.web("#8ab4d8"));
        head.setStroke(Color.web("#1b4778")); head.setStrokeWidth(1.5);
        g.getChildren().add(head);
        Circle eyeL = new Circle(9, 8, 2, Color.web("#1b4778"));
        Circle eyeR = new Circle(14, 8, 2, Color.web("#1b4778"));
        g.getChildren().addAll(eyeL, eyeR);
        Line neck = new Line(11, 13, 11, 15); neck.setStroke(Color.web("#1b4778")); neck.setStrokeWidth(2);
        g.getChildren().add(neck);
        Rectangle body = new Rectangle(3, 15, 17, 10);
        body.setArcWidth(3); body.setArcHeight(3);
        body.setFill(Color.web("#6a94be"));
        body.setStroke(Color.web("#1b4778")); body.setStrokeWidth(1.5);
        g.getChildren().add(body);
        Line armL = new Line(3, 18, 0, 21); armL.setStroke(Color.web("#1b4778")); armL.setStrokeWidth(2);
        Line armR = new Line(20, 18, 23, 21); armR.setStroke(Color.web("#1b4778")); armR.setStrokeWidth(2);
        g.getChildren().addAll(armL, armR);
        Line ant = new Line(11, 3, 11, 0); ant.setStroke(Color.web("#1b4778")); ant.setStrokeWidth(1.5);
        Circle antTip = new Circle(11, 0, 1.5, Color.web("#e24f43"));
        g.getChildren().addAll(ant, antTip);
        return g;
    }

    public static Group trophy() {
        Group g = new Group();
        Polygon cup = new Polygon(5.0, 4.0, 17.0, 4.0, 15.0, 14.0, 13.0, 16.0, 9.0, 16.0, 7.0, 14.0);
        cup.setFill(Color.web("#f5b800"));
        cup.setStroke(Color.web("#a07010")); cup.setStrokeWidth(1.5); cup.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(cup);
        Line stem = new Line(11, 16, 11, 20); stem.setStroke(Color.web("#a07010")); stem.setStrokeWidth(2);
        g.getChildren().add(stem);
        Rectangle base = new Rectangle(6, 20, 10, 3);
        base.setArcWidth(2); base.setArcHeight(2);
        base.setFill(Color.web("#c8901a"));
        base.setStroke(Color.web("#7a5a10")); base.setStrokeWidth(1.2);
        g.getChildren().add(base);
        Line handleL = new Line(5, 6, 2, 9); handleL.setStroke(Color.web("#a07010")); handleL.setStrokeWidth(2); handleL.setStrokeLineCap(StrokeLineCap.ROUND);
        Line handleR = new Line(17, 6, 20, 9); handleR.setStroke(Color.web("#a07010")); handleR.setStrokeWidth(2); handleR.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().addAll(handleL, handleR);
        return g;
    }

    public static Group lock() {
        Group g = new Group();
        Arc shackle = new Arc(11, 10, 5, 5, 0, 180);
        shackle.setType(ArcType.OPEN);
        shackle.setFill(Color.TRANSPARENT);
        shackle.setStroke(Color.web("#5a3a1c")); shackle.setStrokeWidth(2.5);
        g.getChildren().add(shackle);
        Rectangle body = new Rectangle(5, 10, 12, 12);
        body.setArcWidth(3); body.setArcHeight(3);
        body.setFill(Color.web("#c8901a"));
        body.setStroke(Color.web("#7a5a10")); body.setStrokeWidth(1.5);
        g.getChildren().add(body);
        Circle keyhole = new Circle(11, 16, 2.5, Color.web("#7a5a10"));
        g.getChildren().add(keyhole);
        Line keyPin = new Line(11, 17, 11, 20); keyPin.setStroke(Color.web("#7a5a10")); keyPin.setStrokeWidth(2);
        g.getChildren().add(keyPin);
        return g;
    }

    public static Group warning() {
        Group g = new Group();
        Polygon tri = new Polygon(11.0, 2.0, 22.0, 20.0, 0.0, 20.0);
        tri.setFill(Color.web("#f5b800"));
        tri.setStroke(Color.web("#a07010")); tri.setStrokeWidth(1.5); tri.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(tri);
        Line excl = new Line(11, 8, 11, 14); excl.setStroke(Color.web("#5a3a00")); excl.setStrokeWidth(2.5); excl.setStrokeLineCap(StrokeLineCap.ROUND);
        Circle dot = new Circle(11, 17, 1.8, Color.web("#5a3a00"));
        g.getChildren().addAll(excl, dot);
        return g;
    }

    public static Group cardStack() {
        Group g = new Group();
        Rectangle back = new Rectangle(5, 2, 14, 18);
        back.setArcWidth(3); back.setArcHeight(3);
        back.setFill(Color.web("#7a3fb8"));
        back.setStroke(Color.web("#3a1a7a")); back.setStrokeWidth(1.2);
        g.getChildren().add(back);
        Rectangle mid = new Rectangle(3, 4, 14, 18);
        mid.setArcWidth(3); mid.setArcHeight(3);
        mid.setFill(Color.web("#6a30a8"));
        mid.setStroke(Color.web("#3a1a7a")); mid.setStrokeWidth(1.2);
        g.getChildren().add(mid);
        Rectangle front = new Rectangle(1, 6, 14, 18);
        front.setArcWidth(3); front.setArcHeight(3);
        front.setFill(Color.web("#f5e8c0"));
        front.setStroke(Color.web("#7a5a20")); front.setStrokeWidth(1.5);
        g.getChildren().add(front);
        Line deco1 = new Line(4, 12, 12, 12); deco1.setStroke(Color.web("#c8901a")); deco1.setStrokeWidth(1.2);
        Line deco2 = new Line(4, 15, 10, 15); deco2.setStroke(Color.web("#c8901a")); deco2.setStrokeWidth(1.2);
        g.getChildren().addAll(deco1, deco2);
        return g;
    }

    public static Group knight() {
        Group g = new Group();
        Polygon blade = new Polygon(11.0, 1.0, 13.5, 10.0, 11.0, 12.0, 8.5, 10.0);
        blade.setFill(Color.web("#c8c8c0"));
        blade.setStroke(Color.web("#3a3a34")); blade.setStrokeWidth(1.2); blade.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(blade);
        Rectangle guard = new Rectangle(7, 12, 8, 2.5);
        guard.setArcWidth(1); guard.setArcHeight(1);
        guard.setFill(Color.web("#c8901a"));
        guard.setStroke(Color.web("#7a5a10")); guard.setStrokeWidth(1);
        g.getChildren().add(guard);
        Rectangle hilt = new Rectangle(9.5, 14, 3, 9);
        hilt.setArcWidth(2); hilt.setArcHeight(2);
        hilt.setFill(Color.web("#7a5a10"));
        g.getChildren().add(hilt);
        return g;
    }

    public static Group scroll() {
        Group g = new Group();
        Rectangle paper = new Rectangle(4, 4, 15, 16);
        paper.setFill(Color.web("#f5e8c0"));
        paper.setStroke(Color.web("#7a5a20")); paper.setStrokeWidth(1.5);
        g.getChildren().add(paper);
        Ellipse rollT = new Ellipse(11, 4, 7, 2.5);
        rollT.setFill(Color.web("#e0cfa0"));
        rollT.setStroke(Color.web("#7a5a20")); rollT.setStrokeWidth(1.2);
        g.getChildren().add(rollT);
        Ellipse rollB = new Ellipse(11, 20, 7, 2.5);
        rollB.setFill(Color.web("#e0cfa0"));
        rollB.setStroke(Color.web("#7a5a20")); rollB.setStrokeWidth(1.2);
        g.getChildren().add(rollB);
        Line line1 = new Line(7, 9, 16, 9); line1.setStroke(Color.web("#a07a30")); line1.setStrokeWidth(1);
        Line line2 = new Line(7, 12, 16, 12); line2.setStroke(Color.web("#a07a30")); line2.setStrokeWidth(1);
        Line line3 = new Line(7, 15, 14, 15); line3.setStroke(Color.web("#a07a30")); line3.setStrokeWidth(1);
        g.getChildren().addAll(line1, line2, line3);
        return g;
    }

    public static Group pipe() {
        Group g = new Group();
        Line h = new Line(2, 11, 20, 11); h.setStroke(Color.web("#e24f43")); h.setStrokeWidth(4); h.setStrokeLineCap(StrokeLineCap.ROUND);
        Line end1 = new Line(2, 8, 2, 14); end1.setStroke(Color.web("#8e251d")); end1.setStrokeWidth(2.5); end1.setStrokeLineCap(StrokeLineCap.ROUND);
        Line end2 = new Line(20, 8, 20, 14); end2.setStroke(Color.web("#8e251d")); end2.setStrokeWidth(2.5); end2.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().addAll(h, end1, end2);
        return g;
    }

    public static Group monitoringPost() {
        Group g = new Group();
        Rectangle body = new Rectangle(6, 11, 10, 9);
        body.setArcWidth(2); body.setArcHeight(2);
        body.setFill(Color.web("#c8c0a0"));
        body.setStroke(Color.web("#5a4a20")); body.setStrokeWidth(1.2);
        g.getChildren().add(body);
        Polygon roof = new Polygon(4.0, 11.0, 11.0, 2.0, 18.0, 11.0);
        roof.setFill(Color.web("#8a6a30"));
        roof.setStroke(Color.web("#3a2a10")); roof.setStrokeWidth(1.2); roof.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(roof);
        return g;
    }

    public static Group laboratory() {
        Group g = new Group();
        Rectangle body = new Rectangle(4, 8, 14, 13);
        body.setArcWidth(2); body.setArcHeight(2);
        body.setFill(Color.web("#c8e0f0"));
        body.setStroke(Color.web("#1b4778")); body.setStrokeWidth(1.5);
        g.getChildren().add(body);
        Rectangle chimney = new Rectangle(14, 2, 4, 8);
        chimney.setFill(Color.web("#6a94be"));
        chimney.setStroke(Color.web("#1b4778")); chimney.setStrokeWidth(1);
        g.getChildren().add(chimney);
        Line window = new Line(7, 17, 15, 17); window.setStroke(Color.web("#1b4778")); window.setStrokeWidth(1.5);
        g.getChildren().add(window);
        return g;
    }

    public static Group music() {
        Group g = new Group();
        Line stem1 = new Line(6, 5, 6, 15); stem1.setStroke(Color.web("#5a3a1c")); stem1.setStrokeWidth(1.8);
        Line stem2 = new Line(12, 3, 12, 13); stem2.setStroke(Color.web("#5a3a1c")); stem2.setStrokeWidth(1.8);
        Line beam = new Line(6, 5, 12, 3); beam.setStroke(Color.web("#5a3a1c")); beam.setStrokeWidth(2);
        Ellipse note1 = new Ellipse(5, 16, 3, 2); note1.setFill(Color.web("#5a3a1c")); note1.setRotate(-20);
        Ellipse note2 = new Ellipse(11, 14, 3, 2); note2.setFill(Color.web("#5a3a1c")); note2.setRotate(-20);
        g.getChildren().addAll(stem1, stem2, beam, note1, note2);
        return g;
    }

    public static Group speaker() {
        Group g = new Group();
        Polygon cone = new Polygon(4.0, 8.0, 10.0, 5.0, 10.0, 17.0, 4.0, 14.0);
        cone.setFill(Color.web("#5a3a1c"));
        cone.setStroke(Color.web("#3a2010")); cone.setStrokeWidth(1.2); cone.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(cone);
        Rectangle box = new Rectangle(4, 8, 6, 6);
        box.setFill(Color.web("#8a6a40"));
        box.setStroke(Color.web("#3a2010")); box.setStrokeWidth(1);
        g.getChildren().add(box);
        Arc wave1 = new Arc(13, 11, 3, 5, -60, 120);
        wave1.setType(ArcType.OPEN);
        wave1.setFill(Color.TRANSPARENT);
        wave1.setStroke(Color.web("#5a3a1c")); wave1.setStrokeWidth(1.5);
        Arc wave2 = new Arc(13, 11, 5, 8, -60, 120);
        wave2.setType(ArcType.OPEN);
        wave2.setFill(Color.TRANSPARENT);
        wave2.setStroke(Color.web("#5a3a1c")); wave2.setStrokeWidth(1.5);
        g.getChildren().addAll(wave1, wave2);
        return g;
    }

    public static Group person() {
        Group g = new Group();
        Circle head = new Circle(11, 7, 5);
        head.setFill(Color.web("#f5d9a8"));
        head.setStroke(Color.web("#8a5a20")); head.setStrokeWidth(1.5);
        g.getChildren().add(head);
        Arc body = new Arc(11, 20, 8, 9, 0, 180);
        body.setType(ArcType.OPEN);
        body.setFill(Color.TRANSPARENT);
        body.setStroke(Color.web("#5a3a1c")); body.setStrokeWidth(2.5);
        g.getChildren().add(body);
        return g;
    }

    public static Group wrench() {
        Group g = new Group();
        Line handle = new Line(4, 18, 14, 8);
        handle.setStroke(Color.web("#5a3a1c")); handle.setStrokeWidth(3.5); handle.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(handle);
        Circle head = new Circle(16, 6, 4);
        head.setFill(Color.TRANSPARENT);
        head.setStroke(Color.web("#5a3a1c")); head.setStrokeWidth(3);
        g.getChildren().add(head);
        return g;
    }

    public static Group flask() {
        Group g = new Group();
        Rectangle neck = new Rectangle(8, 2, 6, 7);
        neck.setFill(Color.web("#c8e0f0"));
        neck.setStroke(Color.web("#1b4778")); neck.setStrokeWidth(1.2);
        g.getChildren().add(neck);
        Polygon bulb = new Polygon(4.0, 22.0, 6.0, 13.0, 8.0, 9.0, 14.0, 9.0, 16.0, 13.0, 18.0, 22.0);
        bulb.setFill(Color.web("#89c4f4"));
        bulb.setStroke(Color.web("#1b4778")); bulb.setStrokeWidth(1.5); bulb.setStrokeLineJoin(StrokeLineJoin.ROUND);
        g.getChildren().add(bulb);
        Circle bubble1 = new Circle(9, 17, 1.5, Color.web("#ffffff"));
        Circle bubble2 = new Circle(13, 19, 2, Color.web("#ffffff"));
        g.getChildren().addAll(bubble1, bubble2);
        return g;
    }

    public static Group question() {
        Group g = new Group();
        Circle bg = new Circle(11, 11, 10);
        bg.setFill(Color.web("#7a3fb8"));
        bg.setStroke(Color.web("#3a1a7a")); bg.setStrokeWidth(1.5);
        g.getChildren().add(bg);
        Arc qArc = new Arc(11, 9, 3.5, 3.5, 30, 200);
        qArc.setType(ArcType.OPEN);
        qArc.setFill(Color.TRANSPARENT);
        qArc.setStroke(Color.WHITE); qArc.setStrokeWidth(2.5); qArc.setStrokeLineCap(StrokeLineCap.ROUND);
        g.getChildren().add(qArc);
        Line drop = new Line(11, 12, 11, 15); drop.setStroke(Color.WHITE); drop.setStrokeWidth(2.5); drop.setStrokeLineCap(StrokeLineCap.ROUND);
        Circle dot = new Circle(11, 17.5, 1.8, Color.WHITE);
        g.getChildren().addAll(drop, dot);
        return g;
    }

    public static Group settings() {
        Group g = new Group();
        Circle outer = new Circle(11, 11, 7);
        outer.setFill(Color.web("#8a8a80"));
        outer.setStroke(Color.web("#3a3a34")); outer.setStrokeWidth(1.5);
        g.getChildren().add(outer);
        Circle inner = new Circle(11, 11, 3.5, Color.web("#d0d0c8"));
        g.getChildren().add(inner);
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            double cx = 11 + 8.5 * Math.cos(angle);
            double cy = 11 + 8.5 * Math.sin(angle);
            Rectangle tooth = new Rectangle(cx - 1.5, cy - 1.5, 3, 3);
            tooth.setFill(Color.web("#8a8a80"));
            tooth.setStroke(Color.web("#3a3a34")); tooth.setStrokeWidth(0.8);
            tooth.setRotate(i * 60);
            g.getChildren().add(tooth);
        }
        return g;
    }
}
