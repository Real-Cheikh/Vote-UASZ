if (typeof window !== 'undefined' && !(window as any).global) {
  (window as any).global = window;
}

import { Injectable } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ElectionResult } from '../models/models';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private resultsSubject = new Subject<ElectionResult>();
  private connected$ = new BehaviorSubject<boolean>(false);

  private electionSubjects = new Map<number, Subject<ElectionResult>>();
  private activeSubscriptions = new Map<number, StompSubscription>();

  constructor() {}

  public isConnected(): boolean {
    return this.connected$.value;
  }

  public getConnectionStatus(): Observable<boolean> {
    return this.connected$.asObservable();
  }

  public connect(): void {
    if (this.stompClient && (this.stompClient.active || this.stompClient.connected)) {
      return;
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(environment.wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // En développement uniquement
      }
    });

    this.stompClient.onConnect = (frame) => {
      this.connected$.next(true);

      // Souscription au canal de synthèse générale
      this.stompClient?.subscribe('/topic/results/summary', (message: IMessage) => {
        try {
          const result: ElectionResult = JSON.parse(message.body);
          this.resultsSubject.next(result);
        } catch (e) {
          console.error('Erreur de désérialisation du message WebSocket', e);
        }
      });

      // Réabonnement automatique à toutes les élections en écoute lors de la connexion ou reconnexion
      this.electionSubjects.forEach((subject, electionId) => {
        this.bindElectionTopic(electionId, subject);
      });
    };

    this.stompClient.onDisconnect = () => {
      this.connected$.next(false);
      this.activeSubscriptions.clear();
    };

    this.stompClient.onWebSocketClose = () => {
      this.connected$.next(false);
      this.activeSubscriptions.clear();
    };

    this.stompClient.onStompError = (frame) => {
      this.connected$.next(false);
    };

    this.stompClient.activate();
  }

  private bindElectionTopic(electionId: number, subject: Subject<ElectionResult>): void {
    if (!this.stompClient || !this.stompClient.connected) return;

    if (this.activeSubscriptions.has(electionId)) {
      try {
        this.activeSubscriptions.get(electionId)?.unsubscribe();
      } catch (e) {}
    }

    const sub = this.stompClient.subscribe(`/topic/results/${electionId}`, (message: IMessage) => {
      try {
        const result: ElectionResult = JSON.parse(message.body);
        subject.next(result);
      } catch (e) {
        console.error('Erreur message WebSocket pour élection', e);
      }
    });

    this.activeSubscriptions.set(electionId, sub);
  }

  public subscribeToElection(electionId: number): Observable<ElectionResult> {
    if (!this.electionSubjects.has(electionId)) {
      this.electionSubjects.set(electionId, new Subject<ElectionResult>());
    }

    const subject = this.electionSubjects.get(electionId)!;

    if (this.stompClient && this.stompClient.connected) {
      this.bindElectionTopic(electionId, subject);
    } else {
      this.connect();
    }

    return subject.asObservable();
  }

  public unsubscribeFromElection(electionId: number): void {
    if (this.activeSubscriptions.has(electionId)) {
      this.activeSubscriptions.get(electionId)?.unsubscribe();
      this.activeSubscriptions.delete(electionId);
    }
    this.electionSubjects.delete(electionId);
  }

  public getLiveResults(): Observable<ElectionResult> {
    return this.resultsSubject.asObservable();
  }

  public disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
      this.connected$.next(false);
      this.activeSubscriptions.clear();
      this.electionSubjects.clear();
    }
  }
}
