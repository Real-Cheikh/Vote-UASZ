if (typeof window !== 'undefined' && !(window as any).global) {
  (window as any).global = window;
}

import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ElectionResult } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client | null = null;
  private resultsSubject = new Subject<ElectionResult>();

  constructor() {}

  public connect(): void {
    if (this.stompClient && this.stompClient.active) {
      return;
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/api/v1/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // En développement uniquement
      }
    });

    this.stompClient.onConnect = (frame) => {
      // Souscription au canal de synthèse générale
      this.stompClient?.subscribe('/topic/results/summary', (message: IMessage) => {
        try {
          const result: ElectionResult = JSON.parse(message.body);
          this.resultsSubject.next(result);
        } catch (e) {
          console.error('Erreur de désérialisation du message WebSocket', e);
        }
      });
    };

    this.stompClient.activate();
  }

  public subscribeToElection(electionId: number): Observable<ElectionResult> {
    const electionSubject = new Subject<ElectionResult>();

    if (this.stompClient && this.stompClient.connected) {
      this.stompClient.subscribe(`/topic/results/${electionId}`, (message: IMessage) => {
        try {
          const result: ElectionResult = JSON.parse(message.body);
          electionSubject.next(result);
        } catch (e) {
          console.error('Erreur message WebSocket pour élection', e);
        }
      });
    }

    return electionSubject.asObservable();
  }

  public getLiveResults(): Observable<ElectionResult> {
    return this.resultsSubject.asObservable();
  }

  public disconnect(): void {
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
  }
}
